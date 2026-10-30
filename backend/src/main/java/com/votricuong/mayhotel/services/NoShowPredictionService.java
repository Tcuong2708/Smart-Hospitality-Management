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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
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
    private final String healthUrl;

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
        String baseUrl = aiServerUrl.replaceAll("/+$", "");
        this.predictionUrl = baseUrl + "/api/v1/ai/predict-noshow";
        this.healthUrl = baseUrl + "/api/v1/ai/health";
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
            String level = risk >= 60 ? "Cao" : risk >= 30 ? "Trung bình" : "Thấp";
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

    /** Kiểm tra cả kết nối và việc hai tệp mô hình No-show đã được nạp. */
    public boolean isAvailable() {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(healthUrl, Map.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) return false;
            return Boolean.TRUE.equals(response.getBody().get("no_show_ready"));
        } catch (Exception ignored) {
            return false;
        }
    }

    private Map<String, Object> buildFeatures(BookingOrder order) {
        long leadTime = 0;
        long weekNights = 0;
        long weekendNights = 0;
        if (order.getBookingDate() != null && order.getExpectedIn() != null) {
            leadTime = Math.max(0, ChronoUnit.DAYS.between(
                    order.getBookingDate().toInstant(), order.getExpectedIn().toInstant()));
        }
        if (order.getExpectedIn() != null && order.getExpectedOut() != null) {
            LocalDate in = order.getExpectedIn().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate out = order.getExpectedOut().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            for (LocalDate date = in; date.isBefore(out); date = date.plusDays(1)) {
                if (date.getDayOfWeek() == DayOfWeek.FRIDAY || date.getDayOfWeek() == DayOfWeek.SATURDAY) {
                    weekendNights++;
                } else {
                    weekNights++;
                }
            }
        }
        List<Invoice> invoices = invoiceRepository.findByBookingId(order.getId());
        boolean hasDeposit = invoices != null && invoices.stream().anyMatch(invoice ->
                Boolean.TRUE.equals(invoice.getIsPaid())
                        || OnlineBookingPolicy.DEPOSIT_PAID.equalsIgnoreCase(invoice.getDepositStatus())
                        || "Paid".equalsIgnoreCase(invoice.getInvoiceStatus()));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("lead_time", leadTime);
        int adults = Math.max(order.getAdultCount() == null
                ? OnlineBookingPolicy.requiredIdentityVerifications(order) : order.getAdultCount(), 1);
        int children = Math.max(order.getChildCount() == null ? 0 : order.getChildCount(), 0);
        int babies = Math.max(order.getInfantCount() == null ? 0 : order.getInfantCount(), 0);
        data.put("adults", adults);
        data.put("children", children);
        data.put("babies", babies);
        data.put("stays_in_week_nights", weekNights);
        data.put("stays_in_weekend_nights", weekendNights);
        data.put("is_deposit", hasDeposit ? 1 : 0);
        data.put("deposit_type", hasDeposit ? "Non Refund" : "No Deposit");
        data.put("total_of_special_requests", order.getNotes() == null || order.getNotes().isBlank() ? 0 : 1);
        String bookingSource = order.getBookingSource() == null ? "UNKNOWN" : order.getBookingSource();
        data.put("booking_source", bookingSource);
        data.put("market_segment", bookingSource);
        data.put("distribution_channel", bookingSource);

        int previousCancellations = 0;
        int previousCompleted = 0;
        if (order.getCustomerId() != null) {
            for (BookingOrder previous : bookingOrderRepository.findByCustomerIdOrderByBookingDateDesc(order.getCustomerId())) {
                if (previous.getId() != null && previous.getId().equals(order.getId())) continue;
                String status = previous.getStatus() == null ? "" : previous.getStatus().toLowerCase();
                if (status.contains("hủy") || status.contains("cancel") || status.contains("không đến")) {
                    previousCancellations++;
                } else if (status.contains("trả phòng") || status.contains("checked-out")
                        || status.contains("completed") || status.contains("hoàn thành")) {
                    previousCompleted++;
                }
            }
        }
        data.put("is_repeated_guest", previousCompleted > 0 ? 1 : 0);
        data.put("previous_cancellations", previousCancellations);
        data.put("previous_bookings_not_canceled", previousCompleted);
        data.put("customer_type", "Transient");

        if (order.getExpectedIn() != null) {
            LocalDate arrival = order.getExpectedIn().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            data.put("arrival_date_year", arrival.getYear());
            data.put("arrival_date_month", arrival.getMonth().name());
            data.put("arrival_date_week_number", arrival.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear()));
            data.put("arrival_date_day_of_month", arrival.getDayOfMonth());
        }
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
