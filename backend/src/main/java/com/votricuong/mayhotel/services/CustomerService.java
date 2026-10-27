package com.votricuong.mayhotel.services;

import com.votricuong.mayhotel.documents.Customer;
import com.votricuong.mayhotel.repositories.CustomerRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final com.votricuong.mayhotel.repositories.BookingOrderRepository bookingOrderRepository;

    public CustomerService(CustomerRepository customerRepository, SequenceGeneratorService sequenceGeneratorService,
                           com.votricuong.mayhotel.repositories.BookingOrderRepository bookingOrderRepository) {
        this.customerRepository = customerRepository;
        this.sequenceGeneratorService = sequenceGeneratorService;
        this.bookingOrderRepository = bookingOrderRepository;
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    public Optional<Customer> getCustomerById(Long id) {
        return customerRepository.findById(id);
    }

    public Customer createCustomer(Customer customer) {
        validate(customer, null);
        customer.setId(sequenceGeneratorService.generateSequence("customers_sequence"));
        if (customer.getPoints() == null) {
            customer.setPoints(0);
        }
        return customerRepository.save(customer);
    }

    public Customer updateCustomer(Long id, Customer customerDetails) throws Exception {
        Optional<Customer> customerOpt = customerRepository.findById(id);
        if (customerOpt.isEmpty()) {
            throw new Exception("Không tìm thấy khách hàng.");
        }
        validate(customerDetails, id);
        Customer customer = customerOpt.get();
        customer.setFullName(customerDetails.getFullName());
        customer.setDob(customerDetails.getDob());
        customer.setCccd(customerDetails.getCccd());
        customer.setPhone(customerDetails.getPhone());
        customer.setEmail(customerDetails.getEmail());
        customer.setAddress(customerDetails.getAddress());
        customer.setNationality(customerDetails.getNationality());
        customer.setQuocTich(customerDetails.getQuocTich());
        customer.setGender(customerDetails.getGender());
        if (customerDetails.getPoints() != null) customer.setPoints(customerDetails.getPoints());
        customer.setExpiryDate(customerDetails.getExpiryDate());
        if (customerDetails.getTierId() != null) customer.setTierId(customerDetails.getTierId());
        
        return customerRepository.save(customer);
    }

    public void deleteCustomer(Long id) throws Exception {
        if (!customerRepository.existsById(id)) {
            throw new Exception("Không tìm thấy khách hàng.");
        }
        if (bookingOrderRepository.existsByCustomerId(id)) {
            throw new Exception("Không thể xóa khách hàng đã có lịch sử đặt phòng");
        }
        customerRepository.deleteById(id);
    }

    private void validate(Customer customer, Long excludedId) {
        if (customer == null) throw new IllegalArgumentException("Dữ liệu khách hàng không được để trống");
        customer.setFullName(com.votricuong.mayhotel.utils.InputValidator.requiredText(
                customer.getFullName(), "Họ và tên", 100));
        customer.setPhone(com.votricuong.mayhotel.utils.InputValidator.phone(customer.getPhone()));
        customer.setEmail(com.votricuong.mayhotel.utils.InputValidator.email(customer.getEmail(), false));
        if (customer.getCccd() != null && !customer.getCccd().isBlank()) {
            String cccd = customer.getCccd().trim();
            if (!cccd.matches("[0-9A-Za-z]{6,20}")) throw new IllegalArgumentException("CCCD/Hộ chiếu không đúng định dạng");
            customer.setCccd(cccd);
        }
        if (customer.getPoints() != null) {
            com.votricuong.mayhotel.utils.InputValidator.range(customer.getPoints(), "Điểm tích lũy", 0, 100_000_000);
        }
        if (customer.getDob() != null && customer.getDob().after(new java.util.Date())) {
            throw new IllegalArgumentException("Ngày sinh không được ở tương lai");
        }
        if (customerRepository.findByPhone(customer.getPhone()).stream().anyMatch(item ->
                !java.util.Objects.equals(item.getId(), excludedId))) {
            throw new IllegalArgumentException("Số điện thoại đã thuộc khách hàng khác");
        }
        if (!customer.getEmail().isBlank() && customerRepository.findByEmail(customer.getEmail()).stream()
                .anyMatch(item -> !java.util.Objects.equals(item.getId(), excludedId))) {
            throw new IllegalArgumentException("Email đã thuộc khách hàng khác");
        }
    }
}
