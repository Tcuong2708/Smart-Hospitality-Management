package com.votricuong.mayhotel.services;

import com.votricuong.mayhotel.documents.Invoice;
import com.votricuong.mayhotel.repositories.InvoiceRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class RevenueReportService {
    public static final ZoneId HOTEL_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    public static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private final InvoiceRepository invoiceRepository;

    public RevenueReportService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    public Report build(String fromRaw, String toRaw) {
        LocalDate now = LocalDate.now(HOTEL_ZONE);
        LocalDate from = parse(fromRaw, LocalDate.of(now.getYear(), 1, 1));
        LocalDate to = parse(toRaw, LocalDate.of(now.getYear(), 12, 31));
        if (from.isAfter(to)) throw new IllegalArgumentException("Ngày bắt đầu không được sau ngày kết thúc");

        Date fromDate = Date.from(from.atStartOfDay(HOTEL_ZONE).toInstant());
        Date toDate = Date.from(to.plusDays(1).atStartOfDay(HOTEL_ZONE).minusNanos(1).toInstant());
        List<Row> rows = invoiceRepository.findPaidCandidatesForRevenue(fromDate, toDate).stream()
                .map(this::toRow)
                .filter(row -> row.getRevenueDate() != null)
                .filter(row -> !row.getRevenueDate().before(fromDate) && !row.getRevenueDate().after(toDate))
                .sorted(Comparator.comparing(Row::getRevenueDate))
                .toList();

        long days = ChronoUnit.DAYS.between(from, to) + 1;
        Group group = days <= 31 ? Group.DAY : days <= 366 ? Group.MONTH : Group.YEAR;
        LinkedHashMap<String, Bucket> buckets = initializeBuckets(from, to, group);
        for (Row row : rows) {
            LocalDate day = row.getRevenueDate().toInstant().atZone(HOTEL_ZONE).toLocalDate();
            String key = bucketKey(day, group);
            Bucket bucket = buckets.computeIfAbsent(key, ignored -> new Bucket(key, 0D, 0L));
            bucket.setRevenue(bucket.getRevenue() + row.getNetAmount());
            bucket.setBookingCount(bucket.getBookingCount() + 1);
        }

        double gross = rows.stream().mapToDouble(Row::getGrossAmount).sum();
        double refunds = rows.stream().mapToDouble(Row::getRefundAmount).sum();
        double net = rows.stream().mapToDouble(Row::getNetAmount).sum();
        long bookings = rows.stream().map(Row::getBookingId).filter(Objects::nonNull).distinct().count();
        return new Report(DISPLAY_DATE.format(from), DISPLAY_DATE.format(to), from, to,
                gross, refunds, net, rows.size(), bookings,
                rows.isEmpty() ? 0D : net / rows.size(), new ArrayList<>(buckets.values()), rows);
    }

    private Row toRow(Invoice invoice) {
        Date revenueDate = effectivePaymentDate(invoice);
        double gross = value(invoice.getTotalAmount());
        double refund = Math.max(0D, value(invoice.getRefundAmount()));
        return new Row(invoice.getId(), invoice.getBookingId(), revenueDate,
                revenueDate == null ? "" : DISPLAY_DATE.format(revenueDate.toInstant().atZone(HOTEL_ZONE).toLocalDate()),
                text(invoice.getGuestName()), text(invoice.getPhone()), gross, refund,
                Math.max(0D, gross - refund), text(invoice.getPayMethod()), text(invoice.getInvoiceStatus()));
    }

    private Date effectivePaymentDate(Invoice invoice) {
        if (!Boolean.TRUE.equals(invoice.getIsPaid())) return null;
        if (invoice.getPaymentHistory() != null) {
            Optional<Date> success = invoice.getPaymentHistory().stream()
                    .filter(Objects::nonNull)
                    .filter(tx -> "SUCCESS".equalsIgnoreCase(tx.getStatus()) || "00".equals(tx.getResponseCode()))
                    .map(Invoice.PaymentTransaction::getPaymentDate)
                    .filter(Objects::nonNull)
                    .max(Date::compareTo);
            if (success.isPresent()) return success.get();
        }
        return invoice.getCreatedAt();
    }

    private LocalDate parse(String raw, LocalDate fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return LocalDate.parse(raw.trim(), DISPLAY_DATE);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Ngày phải đúng định dạng dd-MM-yyyy");
        }
    }

    private LinkedHashMap<String, Bucket> initializeBuckets(LocalDate from, LocalDate to, Group group) {
        LinkedHashMap<String, Bucket> result = new LinkedHashMap<>();
        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            String key = bucketKey(cursor, group);
            result.putIfAbsent(key, new Bucket(key, 0D, 0L));
            cursor = switch (group) {
                case DAY -> cursor.plusDays(1);
                case MONTH -> cursor.withDayOfMonth(1).plusMonths(1);
                case YEAR -> cursor.withDayOfYear(1).plusYears(1);
            };
        }
        return result;
    }

    private String bucketKey(LocalDate day, Group group) {
        return switch (group) {
            case DAY -> DISPLAY_DATE.format(day);
            case MONTH -> String.format("%02d-%04d", day.getMonthValue(), day.getYear());
            case YEAR -> String.valueOf(day.getYear());
        };
    }

    private double value(Double value) { return value == null ? 0D : value; }
    private String text(String value) { return value == null ? "" : value; }
    private enum Group { DAY, MONTH, YEAR }

    @Data
    @AllArgsConstructor
    public static class Report {
        private String fromDisplay;
        private String toDisplay;
        private LocalDate fromDate;
        private LocalDate toDate;
        private double grossRevenue;
        private double refundAmount;
        private double netRevenue;
        private long paidInvoiceCount;
        private long bookingCount;
        private double averageInvoice;
        private List<Bucket> buckets;
        private List<Row> invoices;
    }

    @Data
    @AllArgsConstructor
    public static class Bucket {
        private String label;
        private double revenue;
        private long bookingCount;
    }

    @Data
    @AllArgsConstructor
    public static class Row {
        private Long id;
        private Long bookingId;
        private Date revenueDate;
        private String dateDisplay;
        private String guestName;
        private String phone;
        private double grossAmount;
        private double refundAmount;
        private double netAmount;
        private String payMethod;
        private String status;
    }
}
