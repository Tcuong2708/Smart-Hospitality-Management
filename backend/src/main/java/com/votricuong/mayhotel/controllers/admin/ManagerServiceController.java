package com.votricuong.mayhotel.controllers.admin;

import com.votricuong.mayhotel.controllers.BaseController;
import com.votricuong.mayhotel.documents.Service;
import com.votricuong.mayhotel.repositories.ServiceRepository;
import com.votricuong.mayhotel.services.SequenceGeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/manager")
public class ManagerServiceController extends BaseController {

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private SequenceGeneratorService sequenceGenerator;

    @Autowired
    private com.votricuong.mayhotel.repositories.ServiceTicketRepository serviceTicketRepository;

    @GetMapping("/services")
    public String servicesIndex(Model model) {
        setPageTitle(model, "Quản Lý Dịch Vụ - Manager");
        List<Service> services = serviceRepository.findAll();
        model.addAttribute("services", services);
        model.addAttribute("serviceObj", new Service());
        setExtraCSS(model, "view/manager/services/index :: extra_css");
        setExtraJS(model, "view/manager/services/index :: extra_js");
        return render(model, "view/manager/services/index");
    }

    @GetMapping("/services/create")
    public String createService(Model model) {
        setPageTitle(model, "Thêm Dịch Vụ - Manager");
        model.addAttribute("serviceObj", new Service());
        return render(model, "view/admin/service/create");
    }

    @PostMapping("/services/edit")
    public String editService(Service input, RedirectAttributes redirect) {
        try {
            if (input.getId() == null) throw new IllegalArgumentException("Thiếu mã dịch vụ");
            Service service = serviceRepository.findById(input.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dịch vụ"));
            String name = com.votricuong.mayhotel.utils.InputValidator.requiredText(input.getName(), "Tên dịch vụ", 100);
            String unit = com.votricuong.mayhotel.utils.InputValidator.requiredText(input.getUnit(), "Đơn vị tính", 30);
            Double price = com.votricuong.mayhotel.utils.InputValidator.positiveMoney(input.getPrice(), "Giá dịch vụ");
            boolean duplicate = serviceRepository.findAll().stream().anyMatch(item -> !item.getId().equals(input.getId())
                    && item.getName() != null && item.getName().equalsIgnoreCase(name));
            if (duplicate) throw new IllegalArgumentException("Tên dịch vụ đã tồn tại");
            service.setName(name); service.setUnit(unit); service.setPrice(price);
            serviceRepository.save(service);
            redirect.addFlashAttribute("success", "Cập nhật dịch vụ thành công!");
        } catch (Exception ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/manager/services";
    }

    @PostMapping("/services/create")
    public String storeService(Service service, RedirectAttributes redirect) {
        try {
            service.setName(com.votricuong.mayhotel.utils.InputValidator.requiredText(
                    service.getName(), "Tên dịch vụ", 100));
            service.setUnit(com.votricuong.mayhotel.utils.InputValidator.requiredText(
                    service.getUnit(), "Đơn vị tính", 30));
            service.setPrice(com.votricuong.mayhotel.utils.InputValidator.positiveMoney(
                    service.getPrice(), "Giá dịch vụ"));
            if (serviceRepository.findAll().stream().anyMatch(item -> item.getName() != null
                    && item.getName().equalsIgnoreCase(service.getName()))) {
                throw new IllegalArgumentException("Tên dịch vụ đã tồn tại");
            }
            service.setId(sequenceGenerator.generateSequence("services_sequence"));
            serviceRepository.save(service);
            redirect.addFlashAttribute("success", "Thêm dịch vụ thành công!");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/manager/services";
    }

    @PostMapping("/services/delete/{id}")
    public String deleteService(@PathVariable Long id, RedirectAttributes redirect) {
        if (!serviceRepository.existsById(id)) redirect.addFlashAttribute("error", "Không tìm thấy dịch vụ.");
        else if (serviceTicketRepository.existsByServiceId(id))
            redirect.addFlashAttribute("error", "Không thể xóa dịch vụ đã phát sinh giao dịch.");
        else {
            serviceRepository.deleteById(id);
            redirect.addFlashAttribute("success", "Xóa dịch vụ thành công!");
        }
        return "redirect:/manager/services";
    }
}
