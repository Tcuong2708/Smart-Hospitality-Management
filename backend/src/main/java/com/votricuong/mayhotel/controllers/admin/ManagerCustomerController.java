package com.votricuong.mayhotel.controllers.admin;

import com.votricuong.mayhotel.controllers.BaseController;
import com.votricuong.mayhotel.documents.Customer;
import com.votricuong.mayhotel.services.CustomerService;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/manager/customers")
public class ManagerCustomerController extends BaseController {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final CustomerService customerService;

    public ManagerCustomerController(CustomerService customerService) { this.customerService = customerService; }

    @GetMapping
    public String index(Model model) {
        List<CustomerRow> rows = customerService.getAllCustomers().stream().map(this::row).toList();
        setPageTitle(model, "Quản lý khách hàng");
        model.addAttribute("customers", rows);
        model.addAttribute("totalCustomers", rows.size());
        model.addAttribute("loyalCustomers", rows.stream().filter(item -> item.getPoints() > 0).count());
        model.addAttribute("linkedCustomers", rows.stream().filter(CustomerRow::isLinkedAccount).count());
        setExtraCSS(model, "view/Manager/customers/index :: extra_css");
        setExtraJS(model, "view/Manager/customers/index :: extra_js");
        return render(model, "view/Manager/customers/index");
    }

    @PostMapping("/create")
    public String create(CustomerForm form, RedirectAttributes redirect) {
        try {
            customerService.createCustomer(toCustomer(form));
            redirect.addFlashAttribute("success", "Thêm khách hàng thành công!");
        } catch (Exception ex) { redirect.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/manager/customers";
    }

    @PostMapping("/edit")
    public String edit(CustomerForm form, RedirectAttributes redirect) {
        try {
            if (form.getId() == null) throw new IllegalArgumentException("Thiếu mã khách hàng");
            customerService.updateCustomer(form.getId(), toCustomer(form));
            redirect.addFlashAttribute("success", "Cập nhật khách hàng thành công!");
        } catch (Exception ex) { redirect.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/manager/customers";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        try { customerService.deleteCustomer(id); redirect.addFlashAttribute("success", "Xóa khách hàng thành công!"); }
        catch (Exception ex) { redirect.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/manager/customers";
    }

    private Customer toCustomer(CustomerForm form) {
        Date dob = null;
        if (form.getDob() != null && !form.getDob().isBlank()) {
            try { dob = Date.from(LocalDate.parse(form.getDob(), DATE).atStartOfDay(ZONE).toInstant()); }
            catch (Exception ex) { throw new IllegalArgumentException("Ngày sinh phải đúng định dạng dd-MM-yyyy"); }
        }
        return Customer.builder().fullName(form.getFullName()).phone(form.getPhone()).email(form.getEmail())
                .cccd(form.getCccd()).address(form.getAddress()).nationality(form.getNationality())
                .quocTich(form.getNationality()).gender(form.getGender()).dob(dob).build();
    }

    private CustomerRow row(Customer item) {
        String cccd = item.getCccd() == null ? "" : item.getCccd();
        String masked = cccd.length() <= 4 ? cccd : "••••••••" + cccd.substring(cccd.length() - 4);
        String dob = item.getDob() == null ? "" : DATE.format(item.getDob().toInstant().atZone(ZONE).toLocalDate());
        return CustomerRow.builder().id(item.getId()).fullName(item.getFullName()).phone(item.getPhone())
                .email(item.getEmail()).cccd(item.getCccd()).maskedCccd(masked).address(item.getAddress())
                .nationality(item.getNationality()).gender(item.getGender()).dob(dob)
                .points(item.getPoints() == null ? 0 : item.getPoints()).tierId(item.getTierId())
                .linkedAccount(item.getUserId() != null).build();
    }

    @Data public static class CustomerForm { private Long id; private String fullName, phone, email, cccd, address, nationality, gender, dob; }
    @Data @Builder public static class CustomerRow {
        private Long id, tierId; private String fullName, phone, email, cccd, maskedCccd, address, nationality, gender, dob;
        private int points; private boolean linkedAccount;
    }
}
