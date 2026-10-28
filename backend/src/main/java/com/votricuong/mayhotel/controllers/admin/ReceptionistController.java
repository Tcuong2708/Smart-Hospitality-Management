package com.votricuong.mayhotel.controllers.admin;

import com.votricuong.mayhotel.controllers.BaseController;
import com.votricuong.mayhotel.documents.BookingOrder;
import com.votricuong.mayhotel.documents.Invoice;
import com.votricuong.mayhotel.documents.Room;
import com.votricuong.mayhotel.documents.RoomType;
import com.votricuong.mayhotel.repositories.InvoiceRepository;
import com.votricuong.mayhotel.repositories.BookingOrderRepository;
import com.votricuong.mayhotel.repositories.RoomRepository;
import com.votricuong.mayhotel.repositories.RoomTypeRepository;
import com.votricuong.mayhotel.services.BookingAvailabilityService;
import com.votricuong.mayhotel.services.LoyaltyService;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Controller;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping({"/receptionist", "/admin"})
public class ReceptionistController extends BaseController {

    private final InvoiceRepository invoiceRepository;
    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final com.votricuong.mayhotel.repositories.BookingDetailRepository bookingDetailRepository;
    private final com.votricuong.mayhotel.services.LoyaltyService loyaltyService;
    private final BookingOrderRepository bookingOrderRepository;
    private final BookingAvailabilityService bookingAvailabilityService;
    private final com.votricuong.mayhotel.services.RoomStatusService roomStatusService;
    private final com.votricuong.mayhotel.services.RoomChangeService roomChangeService;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
    private static final ZoneId HOTEL_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    public ReceptionistController(InvoiceRepository invoiceRepository, RoomRepository roomRepository, RoomTypeRepository roomTypeRepository, com.votricuong.mayhotel.repositories.BookingDetailRepository bookingDetailRepository, com.votricuong.mayhotel.services.LoyaltyService loyaltyService, BookingOrderRepository bookingOrderRepository, BookingAvailabilityService bookingAvailabilityService, com.votricuong.mayhotel.services.RoomStatusService roomStatusService, com.votricuong.mayhotel.services.RoomChangeService roomChangeService) {
        this.invoiceRepository = invoiceRepository;
        this.roomRepository = roomRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.bookingDetailRepository = bookingDetailRepository;
        this.loyaltyService = loyaltyService;
        this.bookingOrderRepository = bookingOrderRepository;
        this.bookingAvailabilityService = bookingAvailabilityService;
        this.roomStatusService = roomStatusService;
        this.roomChangeService = roomChangeService;
    }

    @Data
    @Builder
    public static class InvoiceDTO {
        private Long id;
        private Long bookingId;
        private String hoTen;
        private String sdt;
        private Long maPhong;
        private String tenPhong; // Tên phòng thực tế (vd: P101)
        private Long maLoaiPhong; // Để lọc phòng trống cùng loại
        private String ngayCheckIn;
        private String ngayCheckOut;
        private Double totalPrice;
        private Long idTaiKhoan;
        private String ghiChu;
        @com.fasterxml.jackson.annotation.JsonProperty("isVerified")
        private boolean isVerified;
    }

    @Data
    @Builder
    public static class RoomMapDTO {
        private Long id;
        private String name;
        private String maLoai;
        private Double price;
        private Integer maTrangThai;
        private Integer sucChua;
    }

    @Data
    @Builder
    public static class CheckInRoomDTO {
        private Long id;
        private String name;
        private String label;
        private Long roomTypeId;
        private String type;
        private String view;
        private boolean occupied;
    }

    private CheckInRoomDTO mapToCheckInRoomDTO(Room room,
                                               Map<Long, RoomType> roomTypes,
                                               boolean occupied) {
        RoomType roomType = roomTypes.get(room.getRoomTypeId());
        String typeName = roomType == null ? "" : roomType.getName();
        String normalizedType = typeName == null ? "" : typeName.toLowerCase(java.util.Locale.ROOT);
        String displayType;
        if (normalizedType.contains("tiêu chuẩn") || normalizedType.contains("standard")) {
            displayType = "Standard";
        } else if (normalizedType.contains("deluxe")) {
            displayType = "Deluxe";
        } else if (normalizedType.contains("suite")) {
            displayType = "Suite";
        } else if (typeName != null && !typeName.isBlank()) {
            displayType = typeName.replaceFirst("(?i)^Phòng\\s+", "");
        } else {
            displayType = "Chưa phân loại";
        }

        String roomName = room.getName() == null ? "" : room.getName();
        String roomLabel = roomName.replaceAll("\\D+", "");
        if (roomLabel.isBlank()) {
            roomLabel = String.valueOf(room.getId());
        }

        return CheckInRoomDTO.builder()
                .id(room.getId())
                .name(roomName)
                .label(roomLabel)
                .roomTypeId(room.getRoomTypeId())
                .type(displayType)
                .view(room.getViewDirection())
                .occupied(occupied)
                .build();
    }

    private InvoiceDTO mapToDTO(Invoice inv) {
        String tenPhongStr = "";
        Long maLoai = null;
        Long assignedRoomId = inv.getRoomId();
        if (assignedRoomId == null && inv.getBookingId() != null) {
            List<com.votricuong.mayhotel.documents.BookingDetail> details = bookingDetailRepository.findByBookingId(inv.getBookingId());
            if (details != null && !details.isEmpty() && details.get(0).getRoomId() != null) {
                assignedRoomId = details.get(0).getRoomId();
            }
        }
        if (assignedRoomId != null) {
            Room r = roomRepository.findById(assignedRoomId).orElse(null);
            if (r != null) {
                tenPhongStr = r.getName();
                maLoai = r.getRoomTypeId();
            }
        }
        
        boolean isVerified = inv.getBookingId() != null
                && bookingOrderRepository.findById(inv.getBookingId())
                        .map(order -> Boolean.TRUE.equals(order.getIsFaceVerified()))
                        .orElse(false);

        return InvoiceDTO.builder()
                .id(inv.getId())
                .bookingId(inv.getBookingId())
                .hoTen(inv.getGuestName())
                .sdt(inv.getPhone())
                .maPhong(assignedRoomId)
                .tenPhong(tenPhongStr.isEmpty() && assignedRoomId != null ? ("Phòng " + assignedRoomId) : tenPhongStr)
                .maLoaiPhong(maLoai)
                .ngayCheckIn(inv.getCheckInDate() != null ? dateFormat.format(inv.getCheckInDate()) : "")
                .ngayCheckOut(inv.getCheckOutDate() != null ? dateFormat.format(inv.getCheckOutDate()) : "")
                .totalPrice(inv.getTotalAmount() != null ? inv.getTotalAmount() : 0.0)
                .idTaiKhoan(inv.getUserId())
                .ghiChu(inv.getNote())
                .isVerified(isVerified)
                .build();
    }

    // 0. GET /room-map: Hiển thị sơ đồ phòng
    @GetMapping("/room-map")
    public String roomMapView(@RequestParam(value = "date", required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate requestedDate,
                              Model model) {
        setPageTitle(model, "Sơ đồ phòng trực quan");

        LocalDate today = LocalDate.now(HOTEL_ZONE);
        LocalDate selectedDate = requestedDate == null ? today : requestedDate;
        Map<Long, BookingAvailabilityService.RoomTimelineStatus> roomUsage =
                bookingAvailabilityService.findRoomUsageOn(selectedDate);

        Map<Long, RoomType> typeMapFull = roomTypeRepository.findAll().stream()
                .collect(Collectors.toMap(RoomType::getId, t -> t));

        Map<Long, List<RoomMapDTO>> roomMap = roomRepository.findAll().stream().map(room -> {
            Integer trangThai = 1; // 1 = Vacant
            String st = room.getStatus();
            com.votricuong.mayhotel.enums.RoomStatus operationalStatus =
                    com.votricuong.mayhotel.enums.RoomStatus.fromStored(st);
            BookingAvailabilityService.RoomTimelineStatus timelineStatus = roomUsage.get(room.getId());
            if (timelineStatus == BookingAvailabilityService.RoomTimelineStatus.OCCUPIED) {
                trangThai = 2; // 2 = Occupied
            } else if (!selectedDate.isBefore(today)
                    && operationalStatus == com.votricuong.mayhotel.enums.RoomStatus.MAINTENANCE) {
                trangThai = 5; // 5 = Maintenance
            } else if (selectedDate.equals(today)
                    && operationalStatus == com.votricuong.mayhotel.enums.RoomStatus.CLEANING) {
                trangThai = 3; // 3 = Cleaning
            } else if (timelineStatus == BookingAvailabilityService.RoomTimelineStatus.RESERVED) {
                trangThai = 4; // 4 = Reserved on selected date
            } else if (selectedDate.equals(today)
                    && operationalStatus == com.votricuong.mayhotel.enums.RoomStatus.OCCUPIED) {
                trangThai = 2; // Dữ liệu cũ chưa có actual_in vẫn phải thể hiện đang ở hôm nay
            }

            String loai = "Tiêu chuẩn";
            Integer sucChua = 2;
            if (room.getRoomTypeId() != null && typeMapFull.containsKey(room.getRoomTypeId())) {
                RoomType rt = typeMapFull.get(room.getRoomTypeId());
                loai = rt.getName();
                sucChua = rt.getMaxOccupancy() != null ? rt.getMaxOccupancy() : 2;
            }

            return RoomMapDTO.builder()
                    .id(room.getId())
                    .name(room.getName())
                    .maLoai(loai)
                    .price(room.getPrice())
                    .maTrangThai(trangThai)
                    .sucChua(sucChua)
                    .build();
        }).collect(Collectors.groupingBy(dto -> {
            try {
                String numStr = dto.getName().replaceAll("\\D+", "");
                if (numStr.isEmpty()) return 0L;
                return Long.parseLong(numStr) / 100;
            } catch (Exception e) {
                return 0L;
            }
        }, java.util.TreeMap::new, Collectors.toList()));

        model.addAttribute("rooms", roomMap);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("selectedDateDisplay", selectedDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
        model.addAttribute("today", today);
        model.addAttribute("isToday", selectedDate.equals(today));

        // Nạp CSS riêng cho trang này
        setExtraCSS(model, "view/receptionist/room-map/index :: extra_css");
        setExtraJS(model, "view/receptionist/room-map/index :: extra_js");

        // Render template RoomMap
        return render(model, "view/receptionist/room-map/index");
    }

    // 1. GET /check-in: Hiển thị list phòng chờ nhận
    @GetMapping("/check-in")
    public String checkInView(@RequestParam(value = "roomId", required = false) Long roomId, Model model) {
        setPageTitle(model, "Nghiệp vụ Nhận phòng");
        
        List<InvoiceDTO> invoices = invoiceRepository.findAll().stream()
                .filter(inv -> "Reserved".equalsIgnoreCase(inv.getInvoiceStatus()))
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        List<Long> assignedRoomIds = invoices.stream()
                .map(InvoiceDTO::getMaPhong)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        Map<Long, RoomType> roomTypes = roomTypeRepository.findAll().stream()
                .collect(Collectors.toMap(RoomType::getId, type -> type));

        Set<Long> occupiedRoomIds = bookingAvailabilityService.findOccupiedRoomIdsOn(
                LocalDate.now(HOTEL_ZONE));

        List<CheckInRoomDTO> emptyRooms = roomRepository.findAll().stream()
                .filter(r -> occupiedRoomIds.contains(r.getId())
                        || "Trống".equalsIgnoreCase(r.getStatus())
                        || "Vacant".equalsIgnoreCase(r.getStatus())
                        || (assignedRoomIds.contains(r.getId()) && "Reserved".equalsIgnoreCase(r.getStatus())))
                .filter(r -> r.getId() != null)
                .map(room -> mapToCheckInRoomDTO(room, roomTypes, occupiedRoomIds.contains(room.getId())))
                .collect(Collectors.toList());

        model.addAttribute("invoices", invoices);
        model.addAttribute("emptyRooms", emptyRooms);
        model.addAttribute("selectedRoomId", roomId);

        setExtraCSS(model, "view/receptionist/check-in/index :: extra_css");
        setExtraJS(model, "view/receptionist/check-in/index :: extra_js");
        return render(model, "view/receptionist/check-in/index");
    }

    // 2. GET /checkin/execute/{id}: Xử lý nhận phòng
    @GetMapping("/checkin/execute/{id}")
    public String executeCheckIn(@PathVariable("id") Long id,
                                 @RequestParam(value = "maPhong", required = true) Long maPhong,
                                 @RequestParam(value = "isPaidUpfront", required = false) Boolean isPaidUpfront,
                                 RedirectAttributes redirectAttributes) {
        try {
            Invoice invoice = invoiceRepository.findById(id)
                    .orElseThrow(() -> new Exception("Không tìm thấy phiếu đặt phòng."));
            Room room = roomRepository.findById(maPhong)
                    .orElseThrow(() -> new Exception("Phòng không tồn tại."));

            if (invoice.getBookingId() == null) {
                throw new Exception("Không xác định được đơn đặt phòng để kiểm tra danh tính.");
            }
            BookingOrder verifiedBooking = bookingOrderRepository.findById(invoice.getBookingId())
                    .orElseThrow(() -> new Exception("Không tìm thấy đơn đặt phòng."));
            if (!Boolean.TRUE.equals(verifiedBooking.getIsFaceVerified())) {
                throw new Exception("Khách chưa xác thực danh tính qua Mobile hoặc nhập tay ngoại lệ.");
            }

            if (bookingAvailabilityService.isRoomOccupiedOn(maPhong, LocalDate.now(HOTEL_ZONE))) {
                throw new Exception("Phòng này đang có khách lưu trú, không thể gán cho phiếu check-in.");
            }

            List<com.votricuong.mayhotel.documents.BookingDetail> bookingDetails = invoice.getBookingId() == null
                    ? List.of()
                    : bookingDetailRepository.findByBookingId(invoice.getBookingId());

            Long bookedRoomId = bookingDetails.stream()
                    .map(com.votricuong.mayhotel.documents.BookingDetail::getRoomId)
                    .filter(java.util.Objects::nonNull)
                    .findFirst()
                    .orElse(invoice.getRoomId());
            if (bookedRoomId == null) {
                throw new Exception("Không xác định được loại phòng khách đã đặt.");
            }

            Long bookedRoomTypeId = bookedRoomId.equals(room.getId())
                    ? room.getRoomTypeId()
                    : roomRepository.findById(bookedRoomId)
                            .map(Room::getRoomTypeId)
                            .orElse(null);
            if (bookedRoomTypeId == null || room.getRoomTypeId() == null) {
                throw new Exception("Không xác định được loại phòng để thực hiện check-in.");
            }
            if (!bookedRoomTypeId.equals(room.getRoomTypeId())) {
                throw new Exception("Chỉ được check-in phòng đúng loại khách đã đặt.");
            }

            boolean assignedToThisBooking = maPhong.equals(invoice.getRoomId())
                    || bookingDetails.stream().anyMatch(detail -> maPhong.equals(detail.getRoomId()));
            boolean roomCanBeCheckedIn = "Vacant".equalsIgnoreCase(room.getStatus())
                    || "Trống".equalsIgnoreCase(room.getStatus())
                    || (assignedToThisBooking && "Reserved".equalsIgnoreCase(room.getStatus()));
            if (!roomCanBeCheckedIn) {
                throw new Exception("Phòng này hiện không trống, vui lòng chọn phòng khác.");
            }

            // Cập nhật qua dịch vụ trạng thái để web/mobile nhận sự kiện thời gian thực.
            roomStatusService.markOccupiedForStay(maPhong, null,
                    "Check-in cho đơn #" + verifiedBooking.getId());

            // Cập nhật hóa đơn -> In Use
            invoice.setRoomId(maPhong);
            invoice.setInvoiceStatus("In Use");
            if (Boolean.TRUE.equals(isPaidUpfront)) {
                invoice.setIsPaid(true);
            }
            invoiceRepository.save(invoice);
            if (Boolean.TRUE.equals(isPaidUpfront)) {
                LoyaltyService.PaymentLoyaltyResult loyaltyResult = loyaltyService.finalizePaidInvoice(invoice);
                if (!loyaltyResult.success()) {
                    throw new IllegalStateException(loyaltyResult.message());
                }
            }

            verifiedBooking.setStatus("Đã nhận phòng");
            verifiedBooking.setAutoCancelTime(null);
            bookingOrderRepository.save(verifiedBooking);
            Date checkedInAt = new Date();
            for (com.votricuong.mayhotel.documents.BookingDetail detail : bookingDetails) {
                detail.setRoomId(maPhong);
                detail.setActualIn(checkedInAt);
                bookingDetailRepository.save(detail);
            }

            redirectAttributes.addFlashAttribute("success", "Nhận phòng thành công cho mã #" + id);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/receptionist/check-in";
    }

    // 3. GET /check-out: Hiển thị list phòng đang ở
    @GetMapping("/check-out")
    public String checkOutView(@RequestParam(value = "roomId", required = false) Long roomId, Model model) {
        setPageTitle(model, "Nghiệp vụ Trả phòng");

        List<InvoiceDTO> invoices = invoiceRepository.findAll().stream()
                .filter(inv -> "In Use".equalsIgnoreCase(inv.getInvoiceStatus()))
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        model.addAttribute("invoices", invoices);
        model.addAttribute("selectedRoomId", roomId);

        setExtraCSS(model, "view/receptionist/check-out/index :: extra_css");
        setExtraJS(model, "view/receptionist/check-out/index :: extra_js");
        return render(model, "view/receptionist/check-out/index");
    }

    // 4. POST /checkout-room/execute: Xử lý trả phòng
    @PostMapping("/checkout-room/execute")
    public String executeCheckOut(@RequestParam("maHD") Long maHD,
                                  @RequestParam("phuThu") Double phuThu,
                                  RedirectAttributes redirectAttributes) {
        try {
            Invoice invoice = invoiceRepository.findById(maHD)
                    .orElseThrow(() -> new Exception("Không tìm thấy phiếu đặt phòng."));

            Double currentTotal = invoice.getTotalAmount() != null ? invoice.getTotalAmount() : 0.0;
            if (phuThu == null) phuThu = 0.0;
            
            invoice.setSurcharge(phuThu);
            invoice.setTotalAmount(currentTotal + phuThu);
            invoice.setInvoiceStatus("Completed");
            invoice.setIsPaid(true);
            invoiceRepository.save(invoice);
            LoyaltyService.PaymentLoyaltyResult loyaltyResult = loyaltyService.finalizePaidInvoice(invoice);
            if (!loyaltyResult.success()) {
                throw new IllegalStateException(loyaltyResult.message());
            }

            if (invoice.getBookingId() != null) {
                bookingOrderRepository.findById(invoice.getBookingId()).ifPresent(order -> {
                    order.setStatus("Đã trả phòng");
                    bookingOrderRepository.save(order);
                });
                Date checkedOutAt = new Date();
                for (com.votricuong.mayhotel.documents.BookingDetail detail
                        : bookingDetailRepository.findByBookingId(invoice.getBookingId())) {
                    detail.setActualOut(checkedOutAt);
                    bookingDetailRepository.save(detail);
                    if (detail.getRoomId() != null) {
                        roomStatusService.markCleaningAfterCheckout(detail.getRoomId(), null);
                    }
                }
            } else if (invoice.getRoomId() != null) {
                roomStatusService.markCleaningAfterCheckout(invoice.getRoomId(), null);
            }

            redirectAttributes.addFlashAttribute("success", "Trả phòng thành công. Phòng đã chuyển sang chờ buồng phòng kiểm tra.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/receptionist/check-out";
    }

    // 5. POST /change-room/execute: Xử lý đổi phòng
    @PostMapping("/change-room/execute")
    public String executeChangeRoom(@RequestParam("oldRoomId") Long oldRoomId,
                                    @RequestParam("newRoomId") Long newRoomId,
                                    @RequestParam(value = "reason", required = false) String reason,
                                    @RequestParam(value = "otherReason", required = false) String otherReason,
                                    jakarta.servlet.http.HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        try {
            String finalReason = "Lý do khác".equalsIgnoreCase(reason) ? otherReason : reason;
            Object value = session.getAttribute("user");
            Long performedBy = value instanceof com.votricuong.mayhotel.documents.User user
                    ? user.getId() : null;
            com.votricuong.mayhotel.services.RoomChangeService.Result result =
                    roomChangeService.change(oldRoomId, newRoomId, finalReason, performedBy);
            redirectAttributes.addFlashAttribute("success",
                    "Đổi từ phòng " + result.oldRoomName() + " sang phòng " + result.newRoomName()
                            + " thành công. Chênh lệch hóa đơn: "
                            + String.format("%,.0f VNĐ", result.priceDifference())
                            + ". Phòng cũ đã chuyển sang chờ buồng phòng kiểm tra.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/receptionist/room-map";
    }
}
