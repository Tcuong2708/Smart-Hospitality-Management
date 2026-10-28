package com.votricuong.mayhotel.controllers.admin;

import com.votricuong.mayhotel.controllers.BaseController;
import com.votricuong.mayhotel.documents.RoomType;
import com.votricuong.mayhotel.documents.Room;
import com.votricuong.mayhotel.enums.RoomStatus;
import com.votricuong.mayhotel.repositories.RoomRepository;
import com.votricuong.mayhotel.repositories.RoomTypeRepository;
import com.votricuong.mayhotel.services.BookingAvailabilityService;
import com.votricuong.mayhotel.services.CloudinaryService;
import com.votricuong.mayhotel.services.SequenceGeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/manager")
public class ManagerCategoryController extends BaseController {
    private static final String CLOUDINARY_FOLDER = "may_hotel/categories";
    private static final ZoneId HOTEL_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter DISPLAY_TIMESTAMP = DateTimeFormatter.ofPattern("HH:mm:ss dd-MM-yyyy");

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private SequenceGeneratorService sequenceGenerator;

    @Autowired
    private com.votricuong.mayhotel.services.RoomTypeService roomTypeService;

    @Autowired
    private BookingAvailabilityService bookingAvailabilityService;

    @Autowired
    private CloudinaryService cloudinaryService;

    @GetMapping("/categories")
    public String categoriesIndex(
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate requestedDate,
            Model model) {
        setPageTitle(model, "Quản Lý Loại Phòng");
        List<RoomType> categories = roomTypeRepository.findAll();
        List<Room> allRooms = roomRepository.findAll();
        LocalDate selectedDate = requestedDate == null ? LocalDate.now(HOTEL_ZONE) : requestedDate;
        Map<Long, RoomStatus> statuses = bookingAvailabilityService.findRoomStatusesOn(selectedDate, allRooms);
        
        for (RoomType category : categories) {
            List<Room> roomsInType = allRooms.stream()
                    .filter(room -> category.getId().equals(room.getRoomTypeId()))
                    .toList();
            int total = roomsInType.size();
            int available = (int) roomsInType.stream()
                    .filter(room -> statuses.get(room.getId()) == RoomStatus.VACANT)
                    .count();
            category.setTotalRoomsCount(total);
            category.setAvailableRoomsCount(available);
            
            if (category.getQuantity() == null) {
                category.setQuantity(total);
            }
        }
        
        model.addAttribute("categories", categories);
        model.addAttribute("category", new RoomType());
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("selectedDateText", selectedDate.format(DISPLAY_DATE));
        model.addAttribute("isToday", selectedDate.equals(LocalDate.now(HOTEL_ZONE)));
        model.addAttribute("refreshedAtText", ZonedDateTime.now(HOTEL_ZONE).format(DISPLAY_TIMESTAMP));
        model.addAttribute("cloudinaryEnabled", cloudinaryService.isConfigured());
        setExtraCSS(model, "view/Admin/Category/index :: extra_css");
        return render(model, "view/Admin/Category/index");
    }

    @GetMapping("/categories/create")
    public String createCategory(Model model) {
        setPageTitle(model, "Thêm Loại Phòng - Manager");
        model.addAttribute("category", new RoomType());
        model.addAttribute("cloudinaryEnabled", cloudinaryService.isConfigured());
        return render(model, "view/Admin/Category/create");
    }

    @PostMapping("/categories/create")
    public String storeCategory(
            RoomType category,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            RedirectAttributes redirect) {
        CloudinaryService.UploadResult uploaded = null;
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
            if (hasImage(imageFile)) {
                uploaded = cloudinaryService.uploadImage(imageFile, CLOUDINARY_FOLDER);
                category.setImageUrl(uploaded.secureUrl());
            }
            category.setId(sequenceGenerator.generateSequence("room_types_sequence"));
            roomTypeRepository.save(category);
            redirect.addFlashAttribute("success", "Thêm loại phòng thành công!");
        } catch (IllegalArgumentException ex) {
            cleanupUpload(uploaded);
            redirect.addFlashAttribute("error", ex.getMessage());
        } catch (Exception ex) {
            cleanupUpload(uploaded);
            redirect.addFlashAttribute("error", "Không thể thêm loại phòng: " + safeMessage(ex));
        }
        return "redirect:/manager/categories";
    }

    @PostMapping("/categories/edit")
    public String editCategory(
            RoomType category,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            RedirectAttributes redirect) {
        CloudinaryService.UploadResult uploaded = null;
        try {
            if (category.getId() == null) throw new IllegalArgumentException("Thiếu mã loại phòng");
            RoomType existing = roomTypeRepository.findById(category.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy loại phòng."));
            String oldImageUrl = existing.getImageUrl();
            category.setImageUrl(oldImageUrl);
            if (category.getQuantity() == null) category.setQuantity(existing.getQuantity());
            if (hasImage(imageFile)) {
                uploaded = cloudinaryService.uploadImage(imageFile, CLOUDINARY_FOLDER);
                category.setImageUrl(uploaded.secureUrl());
            }
            roomTypeService.updateRoomType(category.getId(), category);
            if (uploaded != null) {
                String oldPublicId = cloudinaryService.extractPublicId(oldImageUrl);
                if (oldPublicId != null && !cloudinaryService.deleteImage(oldPublicId)) {
                    redirect.addFlashAttribute("warning", "Đã cập nhật loại phòng nhưng chưa xóa được ảnh cũ.");
                }
            }
            if (!redirect.getFlashAttributes().containsKey("warning")) {
                redirect.addFlashAttribute("success", "Cập nhật loại phòng thành công!");
            }
        } catch (Exception ex) {
            cleanupUpload(uploaded);
            redirect.addFlashAttribute("error", safeMessage(ex));
        }
        return "redirect:/manager/categories";
    }

    @PostMapping("/categories/delete/{id}")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirect) {
        RoomType category = roomTypeRepository.findById(id).orElse(null);
        if (category == null) redirect.addFlashAttribute("error", "Không tìm thấy loại phòng.");
        else if (!roomRepository.findByRoomTypeId(id).isEmpty())
            redirect.addFlashAttribute("error", "Không thể xóa loại phòng đang được sử dụng bởi phòng.");
        else {
            String publicId = cloudinaryService.extractPublicId(category.getImageUrl());
            roomTypeRepository.deleteById(id);
            if (publicId != null && !cloudinaryService.deleteImage(publicId)) {
                redirect.addFlashAttribute("warning", "Đã xóa loại phòng nhưng chưa xóa được ảnh trên Cloudinary.");
            } else {
                redirect.addFlashAttribute("success", "Xóa loại phòng thành công!");
            }
        }
        return "redirect:/manager/categories";
    }

    private boolean hasImage(MultipartFile imageFile) {
        return imageFile != null && !imageFile.isEmpty();
    }

    private void cleanupUpload(CloudinaryService.UploadResult uploaded) {
        if (uploaded != null) cloudinaryService.deleteImage(uploaded.publicId());
    }

    private String safeMessage(Exception ex) {
        return ex.getMessage() == null || ex.getMessage().isBlank()
                ? "Lỗi không xác định" : ex.getMessage();
    }
}
