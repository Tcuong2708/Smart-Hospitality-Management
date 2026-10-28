package com.votricuong.mayhotel.controllers.admin;

import com.votricuong.mayhotel.controllers.BaseController;
import com.votricuong.mayhotel.documents.Room;
import com.votricuong.mayhotel.documents.RoomType;
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
import java.util.stream.Collectors;

@Controller
@RequestMapping("/manager")
public class ManagerRoomController extends BaseController {
    private static final String CLOUDINARY_FOLDER = "may_hotel/rooms";
    private static final ZoneId HOTEL_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter DISPLAY_TIMESTAMP = DateTimeFormatter.ofPattern("HH:mm:ss dd-MM-yyyy");

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private SequenceGeneratorService sequenceGenerator;

    @Autowired
    private com.votricuong.mayhotel.repositories.BookingDetailRepository bookingDetailRepository;

    @Autowired
    private BookingAvailabilityService bookingAvailabilityService;

    @Autowired
    private CloudinaryService cloudinaryService;

    @GetMapping("/rooms")
    public String roomsIndex(
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate requestedDate,
            Model model) {
        setPageTitle(model, "Quản Lý Phòng - Manager");
        
        List<RoomType> roomTypes = roomTypeRepository.findAll();
        Map<Long, RoomType> typeMap = roomTypes.stream()
                .collect(Collectors.toMap(RoomType::getId, t -> t));

        List<Room> rooms = roomRepository.findAll();
        LocalDate selectedDate = requestedDate == null ? LocalDate.now(HOTEL_ZONE) : requestedDate;
        Map<Long, RoomStatus> statuses = bookingAvailabilityService.findRoomStatusesOn(selectedDate, rooms);
        for (Room room : rooms) {
            if (room.getRoomTypeId() != null && typeMap.containsKey(room.getRoomTypeId())) {
                room.setRoomType(typeMap.get(room.getRoomTypeId()));
            }
            room.setDisplayStatus(statuses.getOrDefault(room.getId(), RoomStatus.VACANT).getLabel());
            room.setStatus(RoomStatus.fromStored(room.getStatus()).getLabel());
        }
        
        model.addAttribute("rooms", rooms);
        model.addAttribute("room", new Room());
        model.addAttribute("roomTypes", roomTypes);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("selectedDateText", selectedDate.format(DISPLAY_DATE));
        model.addAttribute("isToday", selectedDate.equals(LocalDate.now(HOTEL_ZONE)));
        model.addAttribute("refreshedAtText", ZonedDateTime.now(HOTEL_ZONE).format(DISPLAY_TIMESTAMP));
        model.addAttribute("cloudinaryEnabled", cloudinaryService.isConfigured());
        setExtraCSS(model, "view/Admin/Room/index :: extra_css");
        return render(model, "view/Admin/Room/index");
    }

    @GetMapping("/rooms/create")
    public String createRoom(Model model) {
        setPageTitle(model, "Thêm Phòng Mới - Manager");
        model.addAttribute("room", new Room());
        model.addAttribute("roomTypes", roomTypeRepository.findAll());
        model.addAttribute("cloudinaryEnabled", cloudinaryService.isConfigured());
        return render(model, "view/Admin/Room/create");
    }

    @PostMapping("/rooms/create")
    public String storeRoom(
            Room room,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            RedirectAttributes redirect) {
        CloudinaryService.UploadResult uploaded = null;
        try {
            validateRoom(room, null);
            if (hasImage(imageFile)) {
                uploaded = cloudinaryService.uploadImage(imageFile, CLOUDINARY_FOLDER);
                room.setImageUrl(uploaded.secureUrl());
            }
            room.setId(sequenceGenerator.generateSequence("rooms_sequence"));
            roomRepository.save(room);
            redirect.addFlashAttribute("success", "Thêm phòng thành công!");
        } catch (IllegalArgumentException ex) {
            cleanupUpload(uploaded);
            redirect.addFlashAttribute("error", ex.getMessage());
        } catch (Exception ex) {
            cleanupUpload(uploaded);
            redirect.addFlashAttribute("error", "Không thể thêm phòng: " + safeMessage(ex));
        }
        return "redirect:/manager/rooms";
    }

    @PostMapping("/rooms/delete/{id}")
    public String deleteRoom(@PathVariable Long id, RedirectAttributes redirect) {
        Room room = roomRepository.findById(id).orElse(null);
        if (room == null) redirect.addFlashAttribute("error", "Không tìm thấy phòng.");
        else if (bookingDetailRepository.existsByRoomId(id))
            redirect.addFlashAttribute("error", "Không thể xóa phòng đã phát sinh đặt phòng; hãy chuyển sang trạng thái bảo trì.");
        else {
            String publicId = cloudinaryService.extractPublicId(room.getImageUrl());
            roomRepository.deleteById(id);
            if (publicId != null && !cloudinaryService.deleteImage(publicId)) {
                redirect.addFlashAttribute("warning", "Đã xóa phòng nhưng chưa xóa được ảnh trên Cloudinary.");
            } else {
                redirect.addFlashAttribute("success", "Xóa phòng thành công!");
            }
        }
        return "redirect:/manager/rooms";
    }

    @PostMapping("/rooms/edit")
    public String editRoom(
            Room room,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            RedirectAttributes redirect) {
        if (room.getId() != null) {
            Room existing = roomRepository.findById(room.getId()).orElse(null);
            if (existing != null) {
                CloudinaryService.UploadResult uploaded = null;
                String oldImageUrl = existing.getImageUrl();
                try {
                    validateRoom(room, room.getId());
                    if (hasImage(imageFile)) {
                        uploaded = cloudinaryService.uploadImage(imageFile, CLOUDINARY_FOLDER);
                    }

                    // Update fields
                    existing.setName(room.getName());
                    existing.setPrice(room.getPrice());
                    existing.setRoomTypeId(room.getRoomTypeId());
                    existing.setMaxExtraBeds(room.getMaxExtraBeds());
                    existing.setStatus(room.getStatus());
                    existing.setDetail(room.getDetail());
                    existing.setNote(room.getNote());
                    existing.setViewDirection(room.getViewDirection());
                    if (uploaded != null) existing.setImageUrl(uploaded.secureUrl());

                    roomRepository.save(existing);
                    if (uploaded != null) {
                        String oldPublicId = cloudinaryService.extractPublicId(oldImageUrl);
                        if (oldPublicId != null && !cloudinaryService.deleteImage(oldPublicId)) {
                            redirect.addFlashAttribute("warning", "Đã cập nhật phòng nhưng chưa xóa được ảnh cũ.");
                        }
                    }
                    if (!redirect.getFlashAttributes().containsKey("warning")) {
                        redirect.addFlashAttribute("success", "Cập nhật phòng thành công!");
                    }
                } catch (IllegalArgumentException ex) {
                    cleanupUpload(uploaded);
                    redirect.addFlashAttribute("error", ex.getMessage());
                } catch (Exception ex) {
                    cleanupUpload(uploaded);
                    redirect.addFlashAttribute("error", "Không thể cập nhật phòng: " + safeMessage(ex));
                }
            }
        }
        return "redirect:/manager/rooms";
    }

    @GetMapping("/rooms/details/{id}")
    public String detailsRoom(@PathVariable Long id, Model model) {
        setPageTitle(model, "Chi Tiết Phòng - Manager");
        Room room = roomRepository.findById(id).orElse(null);
        if (room != null && room.getRoomTypeId() != null) {
            room.setRoomType(roomTypeRepository.findById(room.getRoomTypeId()).orElse(null));
        }
        model.addAttribute("room", room);
        return render(model, "view/Admin/Room/details");
    }

    private void validateRoom(Room room, Long excludedId) {
        room.setName(com.votricuong.mayhotel.utils.InputValidator.requiredText(room.getName(), "Tên phòng", 50));
        room.setPrice(com.votricuong.mayhotel.utils.InputValidator.positiveMoney(room.getPrice(), "Giá phòng"));
        room.setDetail(com.votricuong.mayhotel.utils.InputValidator.optionalText(room.getDetail(), "Mô tả phòng", 1000));
        room.setNote(com.votricuong.mayhotel.utils.InputValidator.optionalText(room.getNote(), "Ghi chú phòng", 500));
        com.votricuong.mayhotel.utils.InputValidator.range(
                room.getMaxExtraBeds() == null ? 0 : room.getMaxExtraBeds(), "Số giường phụ", 0, 10);
        if (room.getRoomTypeId() == null || !roomTypeRepository.existsById(room.getRoomTypeId())) {
            throw new IllegalArgumentException("Loại phòng không tồn tại");
        }
        if (roomRepository.findAll().stream().anyMatch(existing -> existing.getId() != null
                && !existing.getId().equals(excludedId) && existing.getName() != null
                && existing.getName().equalsIgnoreCase(room.getName()))) {
            throw new IllegalArgumentException("Tên phòng đã tồn tại");
        }
        room.setStatus(room.getStatus() == null || room.getStatus().isBlank()
                ? RoomStatus.VACANT.getLabel()
                : RoomStatus.fromApi(room.getStatus()).getLabel());
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
