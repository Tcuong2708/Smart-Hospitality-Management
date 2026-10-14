package com.votricuong.mayhotel.services;

import com.votricuong.mayhotel.documents.Customer;
import com.votricuong.mayhotel.documents.Invoice;
import com.votricuong.mayhotel.documents.LoyalCustomer;
import com.votricuong.mayhotel.documents.LoyaltyPolicy;
import com.votricuong.mayhotel.repositories.CustomerRepository;
import com.votricuong.mayhotel.repositories.InvoiceRepository;
import com.votricuong.mayhotel.repositories.LoyalCustomerRepository;
import com.votricuong.mayhotel.repositories.LoyaltyPolicyRepository;
import org.springframework.stereotype.Service;

@Service
public class LoyaltyService {
    private final CustomerRepository customerRepository;
    private final LoyalCustomerRepository loyalCustomerRepository;
    private final LoyaltyPolicyRepository loyaltyPolicyRepository;
    private final InvoiceRepository invoiceRepository;
    private final SequenceGeneratorService sequenceGeneratorService;

    public LoyaltyService(
            CustomerRepository customerRepository,
            LoyalCustomerRepository loyalCustomerRepository,
            LoyaltyPolicyRepository loyaltyPolicyRepository,
            InvoiceRepository invoiceRepository,
            SequenceGeneratorService sequenceGeneratorService) {
        this.customerRepository = customerRepository;
        this.loyalCustomerRepository = loyalCustomerRepository;
        this.loyaltyPolicyRepository = loyaltyPolicyRepository;
        this.invoiceRepository = invoiceRepository;
        this.sequenceGeneratorService = sequenceGeneratorService;
    }

    public int awardPointsForPaidInvoice(Invoice invoice) {
        if (!Boolean.TRUE.equals(invoice.getIsPaid())
                || Boolean.TRUE.equals(invoice.getLoyaltyPointsAwarded())
                || invoice.getUserId() == null
                || invoice.getTotalAmount() == null
                || invoice.getTotalAmount() <= 0) {
            return 0;
        }

        Customer customer = customerRepository.findFirstByUserIdOrderByIdAsc(invoice.getUserId())
                .orElse(null);
        if (customer == null) {
            return 0;
        }

        LoyaltyPolicy policy = loyaltyPolicyRepository.findById("singleton")
                .orElse(defaultPolicy());
        double moneyPerPoint = policy.getMoneyPerPoint() == null ? 100000.0 : policy.getMoneyPerPoint();
        int earnedPoints = (int) Math.floor(invoice.getTotalAmount() / moneyPerPoint);

        invoice.setEarnedPoints(earnedPoints);
        invoice.setLoyaltyPointsAwarded(true);
        invoiceRepository.save(invoice);

        if (earnedPoints == 0) {
            return 0;
        }

        int currentPoints = customer.getPoints() == null ? 0 : customer.getPoints();
        int totalPoints = currentPoints + earnedPoints;
        customer.setPoints(totalPoints);
        applyCustomerTier(customer, totalPoints, policy);
        customerRepository.save(customer);

        LoyalCustomer loyalCustomer = loyalCustomerRepository.findByCustomerId(customer.getId())
                .orElseGet(() -> LoyalCustomer.builder()
                        .id(sequenceGeneratorService.generateSequence("loyal_customers_sequence"))
                        .customerId(customer.getId())
                        .build());
        loyalCustomer.setTotalPoints(totalPoints);
        loyalCustomer.setTier(tierName(customer.getTierId()));
        loyalCustomerRepository.save(loyalCustomer);
        return earnedPoints;
    }

    public int redeemPoints(Customer customer, int requestedPoints) {
        if (customer == null || requestedPoints <= 0) {
            return 0;
        }

        int currentPoints = customer.getPoints() == null ? 0 : customer.getPoints();
        int redeemedPoints = Math.min(requestedPoints, currentPoints);
        int remainingPoints = currentPoints - redeemedPoints;
        customer.setPoints(remainingPoints);

        LoyaltyPolicy policy = loyaltyPolicyRepository.findById("singleton")
                .orElse(defaultPolicy());
        applyCustomerTier(customer, remainingPoints, policy);
        customerRepository.save(customer);
        syncLoyalCustomer(customer, remainingPoints);
        return redeemedPoints;
    }

    private void syncLoyalCustomer(Customer customer, int totalPoints) {
        LoyalCustomer loyalCustomer = loyalCustomerRepository.findByCustomerId(customer.getId())
                .orElseGet(() -> LoyalCustomer.builder()
                        .id(sequenceGeneratorService.generateSequence("loyal_customers_sequence"))
                        .customerId(customer.getId())
                        .build());
        loyalCustomer.setTotalPoints(totalPoints);
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

    private String tierName(Long tierId) {
        if (Long.valueOf(4L).equals(tierId)) return "Bạch Kim";
        if (Long.valueOf(3L).equals(tierId)) return "Vàng";
        if (Long.valueOf(2L).equals(tierId)) return "Bạc";
        return "Đồng";
    }

    private LoyaltyPolicy defaultPolicy() {
        return LoyaltyPolicy.builder()
                .id("singleton")
                .moneyPerPoint(100000.0)
                .silverThreshold(100)
                .goldThreshold(500)
                .platinumThreshold(2000)
                .build();
    }
}
