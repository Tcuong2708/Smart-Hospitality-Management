package com.votricuong.mayhotel.services;

import com.votricuong.mayhotel.documents.Customer;
import com.votricuong.mayhotel.documents.Invoice;
import com.votricuong.mayhotel.documents.LoyalCustomer;
import com.votricuong.mayhotel.documents.LoyaltyPolicy;
import com.votricuong.mayhotel.documents.PointTransaction;
import com.votricuong.mayhotel.repositories.CustomerRepository;
import com.votricuong.mayhotel.repositories.InvoiceRepository;
import com.votricuong.mayhotel.repositories.LoyalCustomerRepository;
import com.votricuong.mayhotel.repositories.LoyaltyPolicyRepository;
import com.votricuong.mayhotel.repositories.PointTransactionRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class LoyaltyService {
    private static final Logger log = LoggerFactory.getLogger(LoyaltyService.class);
    private final CustomerRepository customerRepository;
    private final LoyalCustomerRepository loyalCustomerRepository;
    private final LoyaltyPolicyRepository loyaltyPolicyRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final InvoiceRepository invoiceRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final MongoTemplate mongoTemplate;
    private final PromotionService promotionService;

    public LoyaltyService(CustomerRepository customerRepository,
                          LoyalCustomerRepository loyalCustomerRepository,
                          LoyaltyPolicyRepository loyaltyPolicyRepository,
                          PointTransactionRepository pointTransactionRepository,
                          InvoiceRepository invoiceRepository,
                          SequenceGeneratorService sequenceGeneratorService,
                          MongoTemplate mongoTemplate,
                          PromotionService promotionService) {
        this.customerRepository = customerRepository;
        this.loyalCustomerRepository = loyalCustomerRepository;
        this.loyaltyPolicyRepository = loyaltyPolicyRepository;
        this.pointTransactionRepository = pointTransactionRepository;
        this.invoiceRepository = invoiceRepository;
        this.sequenceGeneratorService = sequenceGeneratorService;
        this.mongoTemplate = mongoTemplate;
        this.promotionService = promotionService;
    }

    /**
     * Chốt một hóa đơn đã thanh toán. Điểm đổi được trừ ở đây, không phải lúc tạo booking.
     * Hàm synchronized bảo vệ retry đồng thời trong cùng instance; referenceId bảo vệ retry lặp.
     */
    public synchronized PaymentLoyaltyResult finalizePaidInvoice(Invoice invoice) {
        if (invoice == null || invoice.getId() == null) {
            return PaymentLoyaltyResult.failure("Hóa đơn không hợp lệ.");
        }
        if (Boolean.TRUE.equals(invoice.getIsPaid())
                && Boolean.TRUE.equals(invoice.getLoyaltyFinalized())) {
            int earned = Boolean.TRUE.equals(invoice.getLoyaltyPointsAwarded())
                    ? (invoice.getEarnedPoints() == null ? 0 : invoice.getEarnedPoints())
                    : awardPointsForPaidInvoice(invoice);
            return PaymentLoyaltyResult.success(
                    invoice.getRedeemedPoints() == null ? 0 : invoice.getRedeemedPoints(),
                    earned);
        }

        int requested = invoice.getPointsToRedeem() == null ? 0 : invoice.getPointsToRedeem();
        com.votricuong.mayhotel.documents.Promotion reservedVoucher = invoice.getPromoId() == null
                ? null : promotionService.findById(invoice.getPromoId()).orElse(null);
        if (invoice.getPromoId() != null && reservedVoucher == null) {
            return PaymentLoyaltyResult.failure("Không tìm thấy chương trình khuyến mãi.");
        }
        boolean voucherReserved = reservedVoucher == null || promotionService.reserveVoucher(reservedVoucher, invoice);
        if (!voucherReserved) {
            return PaymentLoyaltyResult.failure("Voucher đã hết lượt sử dụng.");
        }
        if (requested > 0) {
            Customer customer = findCustomer(invoice);
            if (customer == null) {
                promotionService.releaseVoucher(reservedVoucher);
                return PaymentLoyaltyResult.failure("Không tìm thấy khách hàng để trừ điểm.");
            }
            int redeemed = redeemPoints(customer, requested, invoice.getId());
            if (redeemed != requested) {
                promotionService.releaseVoucher(reservedVoucher);
                return PaymentLoyaltyResult.failure("Số dư điểm đã thay đổi, vui lòng kiểm tra lại.");
            }
            invoice.setRedeemedPoints(redeemed);
        }

        invoice.setIsPaid(true);
        invoice.setLoyaltyFinalized(true);
        invoiceRepository.save(invoice);
        int earned = awardPointsForPaidInvoice(invoice);
        return PaymentLoyaltyResult.success(requested, earned);
    }

    public synchronized int awardPointsForPaidInvoice(Invoice invoice) {
        if (invoice == null || invoice.getId() == null
                || !Boolean.TRUE.equals(invoice.getIsPaid())
                || invoice.getTotalAmount() == null
                || invoice.getTotalAmount() <= 0) {
            return 0;
        }

        String referenceId = "invoice:" + invoice.getId() + ":earn";
        PointTransaction existing = pointTransactionRepository.findByReferenceId(referenceId).orElse(null);
        if (existing != null) {
            int points = Math.max(0, existing.getPoints());
            if (!Boolean.TRUE.equals(invoice.getLoyaltyPointsAwarded())
                    || !Integer.valueOf(points).equals(invoice.getEarnedPoints())) {
                invoice.setEarnedPoints(points);
                invoice.setLoyaltyPointsAwarded(true);
                invoiceRepository.save(invoice);
            }
            return points;
        }

        Customer customer = findCustomer(invoice);
        if (customer == null) {
            log.warn("Không thể tích điểm cho hóa đơn {}: không tìm thấy khách theo tài khoản hoặc số điện thoại.",
                    invoice.getId());
            return 0;
        }
        LoyaltyPolicy policy = currentPolicy();
        int earned = (int) Math.floor(invoice.getTotalAmount() / moneyPerPoint(policy));
        if (earned <= 0) {
            log.info("Hóa đơn {} không phát sinh điểm: số tiền đủ điều kiện {} và tỷ lệ {}.",
                    invoice.getId(), invoice.getTotalAmount(), moneyPerPoint(policy));
            invoice.setEarnedPoints(0);
            invoice.setLoyaltyPointsAwarded(true);
            invoiceRepository.save(invoice);
            return 0;
        }

        Customer updated = mongoTemplate.findAndModify(
                Query.query(Criteria.where("_id").is(customer.getId())),
                new Update().inc("points", earned),
                org.springframework.data.mongodb.core.FindAndModifyOptions.options().returnNew(true),
                Customer.class);
        if (updated == null) {
            return 0;
        }
        applyCustomerTier(updated, safePoints(updated), policy);
        customerRepository.save(updated);
        syncLoyalCustomer(updated, safePoints(updated));
        saveTransaction(updated, invoice.getId(), referenceId, "EARN", earned,
                "Tích điểm sau khi thanh toán hóa đơn #" + invoice.getId(), safePoints(updated));
        invoice.setEarnedPoints(earned);
        invoice.setLoyaltyPointsAwarded(true);
        invoiceRepository.save(invoice);
        return earned;
    }

    public int redeemPoints(Customer customer, int requestedPoints) {
        return redeemPoints(customer, requestedPoints, null);
    }

    private synchronized int redeemPoints(Customer customer, int requestedPoints, Long invoiceId) {
        if (customer == null || customer.getId() == null || requestedPoints <= 0) {
            return 0;
        }
        String referenceId = invoiceId == null
                ? "manual:" + customer.getId() + ":" + System.nanoTime()
                : "invoice:" + invoiceId + ":redeem";
        PointTransaction existing = pointTransactionRepository.findByReferenceId(referenceId).orElse(null);
        if (existing != null) {
            return Math.max(0, -existing.getPoints());
        }

        Customer updated = mongoTemplate.findAndModify(
                Query.query(Criteria.where("_id").is(customer.getId())
                        .and("points").gte(requestedPoints)),
                new Update().inc("points", -requestedPoints),
                org.springframework.data.mongodb.core.FindAndModifyOptions.options().returnNew(true),
                Customer.class);
        if (updated == null) {
            return 0;
        }
        LoyaltyPolicy policy = currentPolicy();
        applyCustomerTier(updated, safePoints(updated), policy);
        customerRepository.save(updated);
        syncLoyalCustomer(updated, safePoints(updated));
        saveTransaction(updated, invoiceId, referenceId, "REDEEM", -requestedPoints,
                "Sử dụng điểm giảm giá hóa đơn" + (invoiceId == null ? "" : " #" + invoiceId),
                safePoints(updated));
        return requestedPoints;
    }

    private Customer findCustomer(Invoice invoice) {
        if (invoice.getUserId() != null) {
            Customer byUser = customerRepository.findFirstByUserIdOrderByIdAsc(invoice.getUserId()).orElse(null);
            if (byUser != null) return byUser;
        }
        if (invoice.getPhone() != null) {
            return customerRepository.findByPhone(invoice.getPhone()).stream().findFirst().orElse(null);
        }
        return null;
    }

    private void saveTransaction(Customer customer, Long invoiceId, String referenceId, String type,
                                 int points, String description, int balanceAfter) {
        pointTransactionRepository.save(PointTransaction.builder()
                .id(referenceId)
                .customerId(customer.getId())
                .invoiceId(invoiceId)
                .referenceId(referenceId)
                .type(type)
                .points(points)
                .balanceAfter(balanceAfter)
                .description(description)
                .createdAt(new Date())
                .build());
    }

    private void syncLoyalCustomer(Customer customer, int points) {
        LoyalCustomer loyalCustomer = loyalCustomerRepository.findByCustomerId(customer.getId())
                .orElseGet(() -> LoyalCustomer.builder()
                        .id(sequenceGeneratorService.generateSequence("loyal_customers_sequence"))
                        .customerId(customer.getId())
                        .build());
        loyalCustomer.setTotalPoints(points);
        loyalCustomer.setTier(tierName(customer.getTierId()));
        loyalCustomerRepository.save(loyalCustomer);
    }

    private void applyCustomerTier(Customer customer, int points, LoyaltyPolicy policy) {
        if (policy.getPlatinumThreshold() != null && points >= policy.getPlatinumThreshold()) {
            customer.setTierId(4L);
        } else if (policy.getGoldThreshold() != null && points >= policy.getGoldThreshold()) {
            customer.setTierId(3L);
        } else if (policy.getSilverThreshold() != null && points >= policy.getSilverThreshold()) {
            customer.setTierId(2L);
        } else {
            customer.setTierId(1L);
        }
    }

    private int safePoints(Customer customer) {
        return customer.getPoints() == null ? 0 : customer.getPoints();
    }

    private LoyaltyPolicy currentPolicy() {
        LoyaltyPolicy policy = loyaltyPolicyRepository.findById("singleton")
                .orElseGet(LoyaltyPolicy::defaultPolicy);
        if (policy.usesLegacyThresholds()) {
            policy.upgradeLegacyThresholds();
            loyaltyPolicyRepository.save(policy);
        }
        return policy;
    }

    private double moneyPerPoint(LoyaltyPolicy policy) {
        return policy.getMoneyPerPoint() == null || policy.getMoneyPerPoint() <= 0
                ? 100000.0 : policy.getMoneyPerPoint();
    }

    private String tierName(Long tierId) {
        if (Long.valueOf(4L).equals(tierId)) return "Bạch Kim";
        if (Long.valueOf(3L).equals(tierId)) return "Vàng";
        if (Long.valueOf(2L).equals(tierId)) return "Bạc";
        return "Đồng";
    }

    private LoyaltyPolicy defaultPolicy() {
        return LoyaltyPolicy.defaultPolicy();
    }

    public record PaymentLoyaltyResult(boolean success, int redeemedPoints, int earnedPoints, String message) {
        static PaymentLoyaltyResult success(int redeemed, int earned) {
            return new PaymentLoyaltyResult(true, redeemed, earned, null);
        }

        static PaymentLoyaltyResult failure(String message) {
            return new PaymentLoyaltyResult(false, 0, 0, message);
        }
    }
}
