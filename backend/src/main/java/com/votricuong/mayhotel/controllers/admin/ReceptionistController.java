package com.votricuong.mayhotel.controllers.admin;

import com.votricuong.mayhotel.controllers.BaseController;
import com.votricuong.mayhotel.documents.Invoice;
import com.votricuong.mayhotel.documents.Room;
import com.votricuong.mayhotel.documents.RoomType;
import com.votricuong.mayhotel.repositories.InvoiceRepository;
import com.votricuong.mayhotel.repositories.BookingOrderRepository;
import com.votricuong.mayhotel.repositories.RoomRepository;
import com.votricuong.mayhotel.repositories.RoomTypeRepository;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    public ReceptionistController(InvoiceRepository invoiceRepository, RoomRepository roomRepository, RoomTypeRepository roomTypeRepository, com.votricuong.mayhotel.repositories.BookingDetailRepository bookingDetailRepository, com.votricuong.mayhotel.services.LoyaltyService loyaltyService, BookingOrderRepository bookingOrderRepository) {
        this.invoiceRepository = invoiceRepository;
        this.roomRepository = roomRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.bookingDetailRepository = bookingDetailRepository;
        this.loyaltyService = loyaltyService;
        this.bookingOrderRepository = bookingOrderRepository;
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
    }

    private CheckInRoomDTO mapToCheckInRoomDTO(Room room, Map<Long, RoomType> roomTypes) {
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
    public String roomMapView(Model model) {
        setPageTitle(model, "Sơ đồ phòng trực quan");

        Map<Long, RoomType> typeMapFull = roomTypeRepository.findAll().stream()
                .collect(Collectors.toMap(RoomType::getId, t -> t));

        Map<Long, List<RoomMapDTO>> roomMap = roomRepository.findAll().stream().map(room -> {
            Integer trangThai = 1; // 1 = Vacant
            String st = room.getStatus();
            if ("Occupied".equalsIgnoreCase(st) || "In Use".equalsIgnoreCase(st) || "Đang có khách".equalsIgnoreCase(st)) {
                trangThai = 2; // 2 = Occupied
            } else if ("Cleaning".equalsIgnoreCase(st) || "Chờ dọn dẹp".equalsIgnoreCase(st)) {
                trangThai = 3; // 3 = Cleaning
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

        List<Room> emptyRooms = roomRepository.findAll().stream()
                .filter(r -> "Trống".equalsIgnoreCase(r.getStatus()) || "Vacant".equalsIgnoreCase(r.getStatus()))
                .collect(Collectors.toList());
        model.addAttribute("emptyRooms", emptyRooms);

        // Nạp CSS riêng cho trang này
        setExtraCSS(model, "view/Admin/RoomMap/room-map :: extra_css");

        // Render template RoomMap
        return render(model, "view/Admin/RoomMap/room-map");
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

        List<CheckInRoomDTO> emptyRooms = roomRepository.findAll().stream()
                .filter(r -> "Trống".equalsIgnoreCase(r.getStatus())
                        || "Vacant".equalsIgnoreCase(r.getStatus())
                        || (assignedRoomIds.contains(r.getId()) && "Reserved".equalsIgnoreCase(r.getStatus())))
                .map(room -> mapToCheckInRoomDTO(room, roomTypes))
                .collect(Collectors.toList());

        model.addAttribute("invoices", invoices);
        model.addAttribute("emptyRooms", emptyRooms);
        model.addAttribute("selectedRoomId", roomId);

        setExtraCSS(model, "view/Admin/CheckIn/index :: extra_css");
        return render(model, "view/Admin/CheckIn/index");
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

            // Cập nhật trạng thái phòng -> Occupied
            room.setStatus("Occupied");
            roomRepository.save(room);

            // Cập nhật hóa đơn -> In Use
            invoice.setRoomId(maPhong);
            invoice.setInvoiceStatus("In Use");
            if (Boolean.TRUE.equals(isPaidUpfront)) {
                invoice.setIsPaid(true);
            }
            invoiceRepository.save(invoice);
            if (Boolean.TRUE.equals(isPaidUpfront)) {
                loyaltyService.awardPointsForPaidInvoice(invoice);
            }

            if (invoice.getBookingId() != null) {
                bookingOrderRepository.findById(invoice.getBookingId()).ifPresent(order -> {
                    order.setStatus("Đã nhận phòng");
                    order.setAutoCancelTime(null);
                    bookingOrderRepository.save(order);
                });
                Date checkedInAt = new Date();
                for (com.votricuong.mayhotel.documents.BookingDetail detail : bookingDetails) {
                    detail.setRoomId(maPhong);
                    detail.setActualIn(checkedInAt);
                    bookingDetailRepository.save(detail);
                }
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

        setExtraCSS(model, "view/Admin/Checkout/index :: extra_css");
        return render(model, "view/Admin/Checkout/index");
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
            loyaltyService.awardPointsForPaidInvoice(invoice);

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
                }
            }

            if (invoice.getRoomId() != null) {
                roomRepository.findById(invoice.getRoomId()).ifPresent(room -> {
                    room.setStatus("Trống");
                    roomRepository.save(room);
                });
            }

            redirectAttributes.addFlashAttribute("success", "Trả phòng thành công và tất toán mã #" + maHD);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/receptionist/check-out";
    }

    // 5. POST /change-room/execute: Xử lý đổi phòng
    @PostMapping("/change-room/execute")
    public String executeChangeRoom(@RequestParam("oldRoomId") Long oldRoomId,
                                    @RequestParam("newRoomId") Long newRoomId,
                                    RedirectAttributes redirectAttributes) {
        try {
            Invoice invoice = invoiceRepository.findAll().stream()
                    .filter(inv -> "In Use".equalsIgnoreCase(inv.getInvoiceStatus()) && oldRoomId.equals(inv.getRoomId()))
                    .findFirst()
                    .orElseThrow(() -> new Exception("Không tìm thấy thông tin khách đang ở phòng cũ."));

            Room oldRoom = roomRepository.findById(oldRoomId).orElseThrow(() -> new Exception("Phòng cũ không tồn tại."));
            Room newRoom = roomRepository.findById(newRoomId).orElseThrow(() -> new Exception("Phòng mới không tồn tại."));

            if (!"Vacant".equalsIgnoreCase(newRoom.getStatus()) && !"Trống".equalsIgnoreCase(newRoom.getStatus())) {
                throw new Exception("Phòng mới hiện không trống, vui lòng chọn phòng khác.");
            }

            // Đổi phòng
            oldRoom.setStatus("Trống");
            roomRepository.save(oldRoom);

            newRoom.setStatus("Occupied"); // Đang có khách
            roomRepository.save(newRoom);

            invoice.setRoomId(newRoomId);
            invoiceRepository.save(invoice);

            if (invoice.getBookingId() != null) {
                for (com.votricuong.mayhotel.documents.BookingDetail detail
                        : bookingDetailRepository.findByBookingId(invoice.getBookingId())) {
                    detail.setRoomId(newRoomId);
                    bookingDetailRepository.save(detail);
                }
            }

            redirectAttributes.addFlashAttribute("success", "Đổi từ phòng " + oldRoom.getName() + " sang phòng " + newRoom.getName() + " thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/receptionist/room-map";
    }
}
