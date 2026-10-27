package com.votricuong.mayhotel.controllers.admin;

import com.votricuong.mayhotel.controllers.BaseController;
import com.votricuong.mayhotel.documents.RoomType;
import com.votricuong.mayhotel.documents.Room;
import com.votricuong.mayhotel.repositories.RoomRepository;
import com.votricuong.mayhotel.repositories.RoomTypeRepository;
import com.votricuong.mayhotel.services.SequenceGeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/manager")
public class ManagerCategoryController extends BaseController {

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private SequenceGeneratorService sequenceGenerator;

    @Autowired
    private com.votricuong.mayhotel.services.RoomTypeService roomTypeService;

    @GetMapping("/categories")
    public String categoriesIndex(Model model) {
        setPageTitle(model, "Quản Lý Loại Phòng");
        List<RoomType> categories = roomTypeRepository.findAll();
        
        for (RoomType category : categories) {
            List<Room> roomsInType = roomRepository.findByRoomTypeId(category.getId());
            int total = roomsInType.size();
            int available = (int) roomsInType.stream().filter(r -> "Trống".equalsIgnoreCase(r.getStatus())).count();
            category.setTotalRoomsCount(total);
            category.setAvailableRoomsCount(available);
            
            if (category.getQuantity() == null) {
                category.setQuantity(total);
            }
        }
        
        model.addAttribute("categories", categories);
        model.addAttribute("category", new RoomType());
        setExtraCSS(model, "view/Admin/Category/index :: extra_css");
        return render(model, "view/Admin/Category/index");
    }

    @GetMapping("/categories/create")
    public String createCategory(Model model) {
        setPageTitle(model, "Thêm Loại Phòng - Manager");
        model.addAttribute("category", new RoomType());
        return render(model, "view/Admin/Category/create");
    }

    @PostMapping("/categories/create")
    public String storeCategory(RoomType category, RedirectAttributes redirect) {
        try {
            category.setName(com.votricuong.mayhotel.utils.InputValidator.requiredText(
                    category.getName(), "Tên loại phòng", 100));
            category.setMaxOccupancy(com.votricuong.mayhotel.utils.InputValidator.range(
                    category.getMaxOccupancy(), "Sức chứa", 1, 20));
            if (category.getQuantity() != null) {
                com.votricuong.mayhotel.utils.InputValidator.range(category.getQuantity(), "Số lượng phòng", 0, 10000);
            }
            if (roomTypeRepository.findAll().stream().anyMatch(item -> item.getName() != null
                    && item.getName().equalsIgnoreCase(category.getName()))) {
                throw new IllegalArgumentException("Tên loại phòng đã tồn tại");
            }
            category.setId(sequenceGenerator.generateSequence("room_types_sequence"));
            roomTypeRepository.save(category);
            redirect.addFlashAttribute("success", "Thêm loại phòng thành công!");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/manager/categories";
    }

    @PostMapping("/categories/edit")
    public String editCategory(RoomType category, RedirectAttributes redirect) {
        try {
            if (category.getId() == null) throw new IllegalArgumentException("Thiếu mã loại phòng");
            roomTypeService.updateRoomType(category.getId(), category);
            redirect.addFlashAttribute("success", "Cập nhật loại phòng thành công!");
        } catch (Exception ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/manager/categories";
    }

    @PostMapping("/categories/delete/{id}")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirect) {
        if (!roomTypeRepository.existsById(id)) redirect.addFlashAttribute("error", "Không tìm thấy loại phòng.");
        else if (!roomRepository.findByRoomTypeId(id).isEmpty())
            redirect.addFlashAttribute("error", "Không thể xóa loại phòng đang được sử dụng bởi phòng.");
        else {
            roomTypeRepository.deleteById(id);
            redirect.addFlashAttribute("success", "Xóa loại phòng thành công!");
        }
        return "redirect:/manager/categories";
    }
}
