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
        return render(model, "view/Admin/Service/index");
    }

    @GetMapping("/services/create")
    public String createService(Model model) {
        setPageTitle(model, "Thêm Dịch Vụ - Manager");
        model.addAttribute("serviceObj", new Service());
        return render(model, "view/Admin/Service/create");
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

    @GetMapping("/services/delete/{id}")
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
