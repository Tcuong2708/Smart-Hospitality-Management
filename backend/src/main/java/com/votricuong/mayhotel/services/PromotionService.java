package com.votricuong.mayhotel.services;

import com.votricuong.mayhotel.documents.Promotion;
import com.votricuong.mayhotel.documents.Customer;
import com.votricuong.mayhotel.documents.CustomerPromotion;
import com.votricuong.mayhotel.repositories.PromotionRepository;
import com.votricuong.mayhotel.repositories.CustomerPromotionRepository;
import com.votricuong.mayhotel.repositories.CustomerRepository;
import com.votricuong.mayhotel.repositories.InvoiceRepository;
import com.votricuong.mayhotel.documents.Invoice;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.security.SecureRandom;

@Service
public class PromotionService {
    private final PromotionRepository promotionRepository;
    private final MongoTemplate mongoTemplate;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final InvoiceRepository invoiceRepository;
    private final CustomerPromotionRepository customerPromotionRepository;
    private final CustomerRepository customerRepository;
    private final NotificationService notificationService;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    public PromotionService(PromotionRepository promotionRepository, MongoTemplate mongoTemplate,
                            SequenceGeneratorService sequenceGeneratorService,
                            InvoiceRepository invoiceRepository,
                            CustomerPromotionRepository customerPromotionRepository,
                            CustomerRepository customerRepository,
                            NotificationService notificationService) {
        this.promotionRepository = promotionRepository;
        this.mongoTemplate = mongoTemplate;
        this.sequenceGeneratorService = sequenceGeneratorService;
        this.invoiceRepository = invoiceRepository;
        this.customerPromotionRepository = customerPromotionRepository;
        this.customerRepository = customerRepository;
        this.notificationService = notificationService;
    }

    public List<Promotion> search(String query, String type, String status) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return promotionRepository.findAll().stream()
                .filter(p -> q.isEmpty()
                        || contains(p.getName(), q)
                        || contains(p.getCode(), q)
                        || contains(p.getDescription(), q))
                .filter(p -> type == null || type.isBlank() || type.equalsIgnoreCase(normalizedType(p)))
                .filter(p -> status == null || status.isBlank() || status.equalsIgnoreCase(effectiveStatus(p)))
                .sorted(Comparator.comparing(Promotion::getPriority,
                        Comparator.nullsFirst(Comparator.reverseOrder())))
                .toList();
    }

    public Promotion save(Promotion promotion) {
        if (promotion.getName() != null) {
            promotion.setName(promotion.getName().trim());
        }
        if (promotion.getCode() == null || promotion.getCode().isBlank()) {
            promotion.setCode(generateUniqueCode());
        } else {
            promotion.setCode(promotion.getCode().trim().toUpperCase(Locale.ROOT));
        }
        validate(promotion);
        boolean duplicateName = promotion.getId() == null
                ? promotionRepository.existsByNameIgnoreCase(promotion.getName())
                : promotionRepository.existsByNameIgnoreCaseAndIdNot(promotion.getName(), promotion.getId());
        if (duplicateName) {
            throw new IllegalArgumentException("Tên chương trình khuyến mãi đã tồn tại.");
        }
        if (promotion.getCode() != null && !promotion.getCode().isBlank()) {
            boolean duplicateCode = promotion.getId() == null
                    ? promotionRepository.existsByCodeIgnoreCase(promotion.getCode())
                    : promotionRepository.existsByCodeIgnoreCaseAndIdNot(promotion.getCode(), promotion.getId());
            if (duplicateCode) {
                throw new IllegalArgumentException("Mã khuyến mãi đã tồn tại.");
            }
        }
        if (promotion.getId() == null) {
            promotion.setId(sequenceGeneratorService.generateSequence("promotions_sequence"));
            promotion.setCreatedAt(new Date());
        } else if (promotion.getCreatedAt() == null) {
            promotionRepository.findById(promotion.getId())
                    .map(Promotion::getCreatedAt)
                    .ifPresentOrElse(
                            promotion::setCreatedAt,
                            () -> promotion.setCreatedAt(new Date()));
        }
        if (promotion.getUsedCount() == null) promotion.setUsedCount(0);
        if (promotion.getPriority() == null) promotion.setPriority(0);
        if (promotion.getStatus() == null || promotion.getStatus().isBlank()) {
            promotion.setStatus("DRAFT");
        }
        if (promotion.getCombinable() == null) promotion.setCombinable(false);
        if ("VOUCHER".equalsIgnoreCase(normalizedType(promotion))
                && promotion.getMaxUsesPerCustomer() == null) {
            promotion.setMaxUsesPerCustomer(1);
        }
        if (promotion.getVisibility() == null || promotion.getVisibility().isBlank()) {
            promotion.setVisibility("PUBLIC");
        } else {
            promotion.setVisibility(promotion.getVisibility().trim().toUpperCase(Locale.ROOT));
        }
        if (promotion.getAssignedCount() == null) promotion.setAssignedCount(0);
        return promotionRepository.save(promotion);
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < 20; attempt++) {
            StringBuilder suffix = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                suffix.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            }
            String code = "MAY" + suffix;
            if (!promotionRepository.existsByCodeIgnoreCase(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Không thể sinh mã khuyến mãi duy nhất. Vui lòng thử lại.");
    }

    public void disable(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khuyến mãi."));
        promotion.setStatus("DISABLED");
        promotionRepository.save(promotion);
    }

    public Optional<Promotion> findByCode(String code) {
        if (code == null || code.isBlank()) return Optional.empty();
        return promotionRepository.findByCodeIgnoreCase(code.trim());
    }

    public Optional<Promotion> findById(Long id) {
        return id == null ? Optional.empty() : promotionRepository.findById(id);
    }

    public int sendToCustomers(Long promotionId, List<Long> customerIds) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khuyến mãi."));
        if (!"PRIVATE".equalsIgnoreCase(promotion.getVisibility())) {
            throw new IllegalArgumentException("Chỉ khuyến mãi riêng tư mới có thể gửi cho khách hàng.");
        }
        if (promotion.getCode() == null || promotion.getCode().isBlank()) {
            throw new IllegalArgumentException("Khuyến mãi riêng tư phải có mã.");
        }
        int sent = 0;
        for (Long customerId : new LinkedHashSet<>(customerIds)) {
            if (customerId == null
                    || customerPromotionRepository.existsByPromotionIdAndCustomerId(promotionId, customerId)) {
                continue;
            }
            Customer customer = customerRepository.findById(customerId).orElse(null);
            if (customer == null) continue;
            customerPromotionRepository.save(CustomerPromotion.builder()
                    .id(sequenceGeneratorService.generateSequence("customer_promotions_sequence"))
                    .customerId(customerId).promotionId(promotionId).promotionCode(promotion.getCode())
                    .status("UNUSED").assignedAt(new Date()).build());
            notificationService.create(customerId, customer.getUserId(), "PROMOTION_GIFTED",
                    "Bạn nhận được ưu đãi mới",
                    "Mã " + promotion.getCode() + " — " + promotion.getName()
                            + ". Hạn dùng đến " + formatDate(promotion.getEndDate()) + ".",
                    "bi-gift-fill", "/customer/vouchers");
            sent++;
        }
        promotion.setAssignedCount((int) customerPromotionRepository.countByPromotionId(promotionId));
        promotionRepository.save(promotion);
        return sent;
    }

    public boolean canCustomerUse(Long promotionId, Long customerId) {
        return promotionId != null && customerId != null
                && customerPromotionRepository.existsByPromotionIdAndCustomerIdAndStatus(
                        promotionId, customerId, "UNUSED");
    }

    public void markPrivatePromotionUsed(Long promotionId, Long customerId,
                                         Long bookingId, Long invoiceId) {
        if (promotionId == null || customerId == null) return;
        promotionRepository.findById(promotionId)
                .filter(item -> "PRIVATE".equalsIgnoreCase(item.getVisibility()))
                .ifPresent(item -> customerPromotionRepository
                        .findByPromotionIdAndCustomerId(promotionId, customerId)
                        .filter(link -> "UNUSED".equalsIgnoreCase(link.getStatus()))
                        .ifPresent(link -> {
                            link.setStatus("USED");
                            link.setUsedAt(new Date());
                            link.setUsedBookingId(bookingId);
                            link.setUsedInvoiceId(invoiceId);
                            customerPromotionRepository.save(link);
                        }));
    }

    public Quote bestQuote(double roomAmount, int roomCount, int guestCount, int nights,
                           Date bookingDate, Date stayDate, Long roomTypeId, String voucherCode) {
        return bestQuote(roomAmount, roomCount, guestCount, nights, bookingDate, stayDate,
                roomTypeId, voucherCode, null);
    }

    public Quote bestQuote(double roomAmount, int roomCount, int guestCount, int nights,
                           Date bookingDate, Date stayDate, Long roomTypeId, String voucherCode,
                           Long customerId) {
        List<Promotion> candidates = search(null, null, null).stream()
                .filter(p -> isEligible(p, roomAmount, roomCount, guestCount, nights,
                        bookingDate, stayDate, roomTypeId, voucherCode, customerId))
                .toList();
        return candidates.stream()
                .map(p -> new Quote(p, calculateDiscount(p, roomAmount, roomCount, nights),
                        effectiveStatus(p)))
                .max(Comparator.comparing(Quote::discount)
                        .thenComparing(q -> q.promotion().getPriority(),
                                Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElse(new Quote(null, 0, "NONE"));
    }

    public boolean reserveVoucher(Promotion promotion, Invoice invoice) {
        if (promotion == null || !"VOUCHER".equalsIgnoreCase(normalizedType(promotion))) return true;
        if (invoice != null && promotion.getMaxUsesPerCustomer() != null) {
            long usedByCustomer = 0;
            if (invoice.getUserId() != null) {
                usedByCustomer = invoiceRepository.findByPromoIdAndUserId(promotion.getId(), invoice.getUserId())
                        .stream().filter(this::isPaidInvoice).count();
            } else if (invoice.getPhone() != null && !invoice.getPhone().isBlank()) {
                usedByCustomer = invoiceRepository.findByPromoIdAndPhone(promotion.getId(), invoice.getPhone())
                        .stream().filter(this::isPaidInvoice).count();
            }
            if (usedByCustomer >= promotion.getMaxUsesPerCustomer()) return false;
        }
        Query query = Query.query(Criteria.where("_id").is(promotion.getId())
                .and("status").nin("DISABLED", "EXPIRED")
                .and("usedCount").lt(promotion.getMaxUses() == null ? Integer.MAX_VALUE : promotion.getMaxUses()));
        Update update = new Update().inc("usedCount", 1);
        return mongoTemplate.findAndModify(query, update, Promotion.class) != null;
    }

    private boolean isPaidInvoice(Invoice invoice) {
        return Boolean.TRUE.equals(invoice.getIsPaid())
                && !"CANCELLED".equalsIgnoreCase(invoice.getInvoiceStatus())
                && !"CANCELED".equalsIgnoreCase(invoice.getInvoiceStatus());
    }

    public void releaseVoucher(Promotion promotion) {
        if (promotion != null && "VOUCHER".equalsIgnoreCase(normalizedType(promotion))) {
            mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(promotion.getId())
                            .and("usedCount").gt(0)),
                    new Update().inc("usedCount", -1), Promotion.class);
        }
    }

    @Scheduled(fixedDelay = 300000)
    public void refreshStatuses() {
        Date now = new Date();
        for (Promotion promotion : promotionRepository.findAll()) {
            String next = effectiveStatus(promotion, now);
            if (!next.equals(promotion.getStatus())
                    && !"DISABLED".equalsIgnoreCase(promotion.getStatus())) {
                promotion.setStatus(next);
                promotionRepository.save(promotion);
            }
        }
    }

    private boolean isEligible(Promotion p, double roomAmount, int roomCount, int guests, int nights,
                               Date bookingDate, Date stayDate, Long roomTypeId, String voucherCode,
                               Long customerId) {
        if (!"ACTIVE".equalsIgnoreCase(effectiveStatus(p))
                || (p.getMaxUses() != null && number(p.getUsedCount(), 0) >= p.getMaxUses())
                || (p.getMinimumOrderAmount() != null && roomAmount < p.getMinimumOrderAmount())
                || (p.getMinimumRooms() != null && roomCount < p.getMinimumRooms())
                || (p.getMinimumGuests() != null && guests < p.getMinimumGuests())
                || (p.getMinimumNights() != null && nights < p.getMinimumNights())
                || (p.getEligibleRoomTypeIds() != null && !p.getEligibleRoomTypeIds().isEmpty()
                    && !p.getEligibleRoomTypeIds().contains(roomTypeId))
                || !inDateRange(p, Boolean.TRUE.equals(p.getApplyOnBookingDate()) ? bookingDate : stayDate)) {
            return false;
        }
        if (voucherCode != null && !voucherCode.isBlank()) {
            if (p.getCode() == null || !voucherCode.trim().equalsIgnoreCase(p.getCode().trim())) {
                return false;
            }
            if ("PRIVATE".equalsIgnoreCase(p.getVisibility()) && !canCustomerUse(p.getId(), customerId)) {
                return false;
            }
        } else if ("VOUCHER".equalsIgnoreCase(normalizedType(p))) {
            return false;
        } else if ("PRIVATE".equalsIgnoreCase(p.getVisibility())) {
            return false;
        }
        if (p.getAdvanceDays() != null && bookingDate != null && stayDate != null
                && (stayDate.getTime() - bookingDate.getTime()) / 86400000L < p.getAdvanceDays()) {
            return false;
        }
        if (p.getEligibleWeekdays() != null && stayDate != null) {
            int day = Instant.ofEpochMilli(stayDate.getTime()).atZone(ZoneId.systemDefault())
                    .getDayOfWeek().getValue();
            if (!p.getEligibleWeekdays().contains(day)) return false;
        }
        return true;
    }

    private double calculateDiscount(Promotion p, double base, int rooms, int nights) {
        if (p.getTiers() != null && !p.getTiers().isEmpty()) {
            return p.getTiers().stream()
                    .filter(t -> number(t.get("min"), 0) <= (p.getType() != null && p.getType().equals("GROUP") ? rooms : nights))
                    .mapToDouble(t -> discount(base, String.valueOf(t.getOrDefault("discountType", "PERCENT")),
                            number(t.get("value"), 0)))
                    .max().orElse(0);
        }
        return discount(base, p.getDiscountType(), p.getDiscountType() != null
                && p.getDiscountType().equalsIgnoreCase("AMOUNT")
                ? p.getDiscountAmount() : p.getDiscountPercent());
    }

    private double discount(double base, String type, Double value) {
        if (value == null || value <= 0) return 0;
        double result = "AMOUNT".equalsIgnoreCase(type) ? value : base * value / 100.0;
        return Math.max(0, Math.min(base, result));
    }

    private boolean inDateRange(Promotion p, Date date) {
        if (date == null) return true;
        java.time.LocalDate target = toLocalDate(date);
        java.time.LocalDate start = p.getStartDate() == null ? null : toLocalDate(p.getStartDate());
        java.time.LocalDate end = p.getEndDate() == null ? null : toLocalDate(p.getEndDate());
        return (start == null || !target.isBefore(start))
                && (end == null || !target.isAfter(end));
    }

    private String effectiveStatus(Promotion p) {
        return effectiveStatus(p, new Date());
    }

    private String effectiveStatus(Promotion p, Date now) {
        if (p.getStatus() != null && "DISABLED".equalsIgnoreCase(p.getStatus())) return "DISABLED";
        java.time.LocalDate today = toLocalDate(now);
        if (p.getStartDate() != null && today.isBefore(toLocalDate(p.getStartDate()))) return "SCHEDULED";
        if (p.getEndDate() != null && today.isAfter(toLocalDate(p.getEndDate()))) return "EXPIRED";
        return p.getStatus() == null || p.getStatus().isBlank() ? "ACTIVE" : p.getStatus();
    }

    private java.time.LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private String normalizedType(Promotion p) {
        return p.getType() == null || p.getType().isBlank() ? "SPECIAL_DATE" : p.getType();
    }

    private boolean contains(String value, String q) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(q);
    }

    private double number(Object value, double fallback) {
        return value instanceof Number n ? n.doubleValue() : fallback;
    }

    private String formatDate(Date date) {
        if (date == null) return "không giới hạn";
        return new java.text.SimpleDateFormat("dd/MM/yyyy").format(date);
    }

    private void validate(Promotion p) {
        if (p.getName() == null || p.getName().isBlank()) throw new IllegalArgumentException("Tên không được để trống.");
        if (p.getStartDate() != null && p.getEndDate() != null && p.getEndDate().before(p.getStartDate())) {
            throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu.");
        }
        if (p.getDiscountPercent() != null && (p.getDiscountPercent() < 0 || p.getDiscountPercent() > 100)) {
            throw new IllegalArgumentException("Phần trăm giảm phải từ 0 đến 100.");
        }
        if ("VOUCHER".equalsIgnoreCase(normalizedType(p))
                && (p.getCode() == null || p.getCode().isBlank())) {
            throw new IllegalArgumentException("Voucher phải có mã.");
        }
    }

    public record Quote(Promotion promotion, double discount, String status) {}
}
