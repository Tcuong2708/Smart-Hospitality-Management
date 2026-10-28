package com.votricuong.mayhotel.services;

import com.votricuong.mayhotel.documents.BookingOrder;
import com.votricuong.mayhotel.documents.Invoice;
import com.votricuong.mayhotel.documents.RiskAlert;
import com.votricuong.mayhotel.repositories.BookingOrderRepository;
import com.votricuong.mayhotel.repositories.InvoiceRepository;
import com.votricuong.mayhotel.repositories.RiskAlertRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.temporal.ChronoUnit;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.Socket;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class NoShowPredictionService {

    private final BookingOrderRepository bookingOrderRepository;
    private final InvoiceRepository invoiceRepository;
    private final RiskAlertRepository riskAlertRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final RestTemplate restTemplate;
    private final String predictionUrl;

    public NoShowPredictionService(
            BookingOrderRepository bookingOrderRepository,
            InvoiceRepository invoiceRepository,
            RiskAlertRepository riskAlertRepository,
            SequenceGeneratorService sequenceGeneratorService,
            RestTemplate restTemplate,
            @Value("${ai.server.url:http://127.0.0.1:5000}") String aiServerUrl) {
        this.bookingOrderRepository = bookingOrderRepository;
        this.invoiceRepository = invoiceRepository;
        this.riskAlertRepository = riskAlertRepository;
        this.sequenceGeneratorService = sequenceGeneratorService;
        this.restTemplate = restTemplate;
        this.predictionUrl = aiServerUrl.replaceAll("/+$", "") + "/api/v1/ai/predict-noshow";
    }

    public BookingOrder analyzeAndSave(Long bookingId) {
        BookingOrder order = bookingOrderRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu đặt phòng"));
        order.setRiskAnalysisStatus("PENDING");
        bookingOrderRepository.save(order);

        try {
            Map<String, Object> features = buildFeatures(order);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    predictionUrl, Map.of("data", features), Map.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new IllegalStateException("AI không trả về kết quả hợp lệ");
            }
            Map<?, ?> body = response.getBody();
            Object riskValue = body.get("risk_percentage");
            if (!(riskValue instanceof Number)) {
                throw new IllegalStateException("AI thiếu tỷ lệ rủi ro");
            }
            double risk = Math.max(0, Math.min(100, ((Number) riskValue).doubleValue()));
            String level = risk >= 70 ? "Cao" : risk >= 40 ? "Trung bình" : "Thấp";
            String reason = body.get("reason") == null
                    ? buildReason(features, level)
                    : body.get("reason").toString();

            order.setNoShowRiskPercentage(Math.round(risk * 10.0) / 10.0);
            order.setNoShowRiskLevel(level);
            order.setRiskAnalysisStatus("SUCCESS");
            order.setAutoCancelTime(resolveCancelTime(order, "Cao".equals(level) ? 14 : 18));
            bookingOrderRepository.save(order);
            saveAlert(order, risk, level, reason, "SUCCESS");
            return order;
        } catch (Exception ex) {
            order.setRiskAnalysisStatus("FAILED");
            order.setNoShowRiskLevel(null);
            order.setNoShowRiskPercentage(null);
            order.setAutoCancelTime(resolveCancelTime(order, 18));
            bookingOrderRepository.save(order);
            saveAlert(order, null, null, "Không kết nối được dịch vụ AI: " + safeMessage(ex), "FAILED");
            return order;
        }
    }

    /** Kiểm tra nhanh kết nối tới AI, không gọi dự đoán và không làm thay đổi dữ liệu. */
    public boolean isAvailable() {
        try {
            URI uri = URI.create(predictionUrl);
            int port = uri.getPort() > 0 ? uri.getPort() : ("https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80);
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(uri.getHost(), port), 700);
                return true;
            }
        } catch (Exception ignored) {
            return false;
        }
    }

    private Map<String, Object> buildFeatures(BookingOrder order) {
        long leadTime = 0;
        long nights = 1;
        if (order.getBookingDate() != null && order.getExpectedIn() != null) {
            leadTime = Math.max(0, ChronoUnit.DAYS.between(
                    order.getBookingDate().toInstant(), order.getExpectedIn().toInstant()));
        }
        if (order.getExpectedIn() != null && order.getExpectedOut() != null) {
            nights = Math.max(1, ChronoUnit.DAYS.between(
                    order.getExpectedIn().toInstant(), order.getExpectedOut().toInstant()));
        }
        List<Invoice> invoices = invoiceRepository.findByBookingId(order.getId());
        boolean hasDeposit = invoices != null && invoices.stream().anyMatch(invoice ->
                Boolean.TRUE.equals(invoice.getIsPaid())
                        || OnlineBookingPolicy.DEPOSIT_PAID.equalsIgnoreCase(invoice.getDepositStatus())
                        || "Paid".equalsIgnoreCase(invoice.getInvoiceStatus()));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("lead_time", leadTime);
        data.put("adults", OnlineBookingPolicy.requiredIdentityVerifications(order));
        int children = Math.max(order.getChildCount() == null ? 0 : order.getChildCount(), 0)
                + Math.max(order.getInfantCount() == null ? 0 : order.getInfantCount(), 0);
        data.put("children", children);
        data.put("stays_in_week_nights", nights);
        data.put("is_deposit", hasDeposit ? 1 : 0);
        data.put("total_of_special_requests", order.getNotes() == null || order.getNotes().isBlank() ? 0 : 1);
        data.put("booking_source", order.getBookingSource() == null ? "UNKNOWN" : order.getBookingSource());
        return data;
    }

    private void saveAlert(BookingOrder order, Double risk, String level, String reason, String status) {
        RiskAlert alert = riskAlertRepository.findFirstByBookingId(order.getId()).orElseGet(() -> {
            RiskAlert created = new RiskAlert();
            created.setId(sequenceGeneratorService.generateSequence("risk_alerts_sequence"));
            created.setBookingId(order.getId());
            return created;
        });
        alert.setRiskRate(risk);
        alert.setRiskLevel(level);
        alert.setAnalyzeDate(new Date());
        alert.setReason(reason);
        alert.setAnalysisStatus(status);
        riskAlertRepository.save(alert);
    }

    private Date resolveCancelTime(BookingOrder order, int hour) {
        if (order.getExpectedIn() == null) return null;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(order.getExpectedIn());
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private String buildReason(Map<String, Object> data, String level) {
        return "Mức " + level + "; lead time=" + data.get("lead_time")
                + " ngày; đặt cọc=" + data.get("is_deposit")
                + "; nguồn=" + data.get("booking_source");
    }

    private String safeMessage(Exception ex) {
        return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
    }
}
