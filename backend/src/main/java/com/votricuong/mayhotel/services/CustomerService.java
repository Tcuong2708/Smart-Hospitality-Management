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

    public CustomerService(CustomerRepository customerRepository, SequenceGeneratorService sequenceGeneratorService) {
        this.customerRepository = customerRepository;
        this.sequenceGeneratorService = sequenceGeneratorService;
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    public Optional<Customer> getCustomerById(Long id) {
        return customerRepository.findById(id);
    }

    public Customer createCustomer(Customer customer) {
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
        Customer customer = customerOpt.get();
        customer.setFullName(customerDetails.getFullName());
        customer.setDob(customerDetails.getDob());
        customer.setCccd(customerDetails.getCccd());
        customer.setPhone(customerDetails.getPhone());
        customer.setEmail(customerDetails.getEmail());
        if (customerDetails.getPoints() != null) customer.setPoints(customerDetails.getPoints());
        customer.setExpiryDate(customerDetails.getExpiryDate());
        if (customerDetails.getTierId() != null) customer.setTierId(customerDetails.getTierId());
        
        return customerRepository.save(customer);
    }

    public void deleteCustomer(Long id) throws Exception {
        if (!customerRepository.existsById(id)) {
            throw new Exception("Không tìm thấy khách hàng.");
        }
        customerRepository.deleteById(id);
    }
}
