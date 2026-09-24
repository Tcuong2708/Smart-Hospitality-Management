package com.votricuong.mayhotel.controllers.admin;

import com.votricuong.mayhotel.controllers.BaseController;
import com.votricuong.mayhotel.documents.Invoice;
import com.votricuong.mayhotel.documents.Room;
import com.votricuong.mayhotel.documents.RoomType;
import com.votricuong.mayhotel.repositories.InvoiceRepository;
import com.votricuong.mayhotel.repositories.RoomRepository;
import com.votricuong.mayhotel.repositories.RoomTypeRepository;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/receptionist")
public class ReceptionistController extends BaseController {

    private final InvoiceRepository invoiceRepository;
    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    public ReceptionistController(InvoiceRepository invoiceRepository, RoomRepository roomRepository, RoomTypeRepository roomTypeRepository) {
        this.invoiceRepository = invoiceRepository;
        this.roomRepository = roomRepository;
        this.roomTypeRepository = roomTypeRepository;
    }

    @Data
    @Builder
    public static class InvoiceDTO {
        private Long id;
        private String hoTen;
        private String sdt;
        private Long maPhong;
        private String ngayCheckIn;
        private String ngayCheckOut;
        private Double totalPrice;
        private Long idTaiKhoan;
        private String ghiChu;
    }

    @Data
    @Builder
    public static class RoomMapDTO {
        private Long id;
        private String name;
        private String maLoai;
        private Double price;
        private Integer maTrangThai;
    }

    private InvoiceDTO mapToDTO(Invoice inv) {
        return InvoiceDTO.builder()
                .id(inv.getId())
                .hoTen(inv.getGuestName())
                .sdt(inv.getPhone())
                .maPhong(inv.getRoomId())
                .ngayCheckIn(inv.getCheckInDate() != null ? dateFormat.format(inv.getCheckInDate()) : "")
                .ngayCheckOut(inv.getCheckOutDate() != null ? dateFormat.format(inv.getCheckOutDate()) : "")
                .totalPrice(inv.getTotalAmount() != null ? inv.getTotalAmount() : 0.0)
                .idTaiKhoan(inv.getUserId())
                .ghiChu(inv.getNote())
                .build();
    }

    // 0. GET /room-map: Hiển thị sơ đồ phòng
    @GetMapping("/room-map")
    public String roomMapView(Model model) {
        setPageTitle(model, "Sơ đồ phòng trực quan");

        Map<Long, String> typeMap = roomTypeRepository.findAll().stream()
                .collect(Collectors.toMap(RoomType::getId, RoomType::getName));

        Map<Long, List<RoomMapDTO>> roomMap = roomRepository.findAll().stream().map(room -> {
            Integer trangThai = 1; // 1 = Vacant
            if ("Occupied".equalsIgnoreCase(room.getStatus()) || "In Use".equalsIgnoreCase(room.getStatus())) {
                trangThai = 2; // 2 = Occupied
            } else if ("Cleaning".equalsIgnoreCase(room.getStatus())) {
                trangThai = 3; // 3 = Cleaning
            }

            String loai = "Tiêu chuẩn";
            if (room.getRoomTypeId() != null && typeMap.containsKey(room.getRoomTypeId())) {
                loai = typeMap.get(room.getRoomTypeId());
            }

            return RoomMapDTO.builder()
                    .id(room.getId())
                    .name(room.getName())
                    .maLoai(loai)
                    .price(room.getPrice())
                    .maTrangThai(trangThai)
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

        // Nạp CSS riêng cho trang này để hiển thị đúng lưới Grid thay vì dọc
        setExtraCSS(model, "view/Admin/RoomMap/room-map :: extra_css");

        // Render template RoomMap có sẵn
        return render(model, "view/Admin/RoomMap/room-map");
    }

    // 1. GET /check-in: Hiển thị list phòng chờ nhận
    @GetMapping("/check-in")
    public String checkInView(Model model) {
        setPageTitle(model, "Nghiệp vụ Nhận phòng");
        
        List<InvoiceDTO> invoices = invoiceRepository.findAll().stream()
                .filter(inv -> "Reserved".equalsIgnoreCase(inv.getInvoiceStatus()))
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        List<Room> emptyRooms = roomRepository.findAll().stream()
                .filter(r -> "Vacant".equalsIgnoreCase(r.getStatus()))
                .collect(Collectors.toList());

        model.addAttribute("invoices", invoices);
        model.addAttribute("emptyRooms", emptyRooms);

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

            if (!"Vacant".equalsIgnoreCase(room.getStatus())) {
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

            redirectAttributes.addFlashAttribute("success", "Nhận phòng thành công cho mã #" + id);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/receptionist/check-in";
    }

    // 3. GET /check-out: Hiển thị list phòng đang ở
    @GetMapping("/check-out")
    public String checkOutView(Model model) {
        setPageTitle(model, "Nghiệp vụ Trả phòng");

        List<InvoiceDTO> invoices = invoiceRepository.findAll().stream()
                .filter(inv -> "In Use".equalsIgnoreCase(inv.getInvoiceStatus()))
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        model.addAttribute("invoices", invoices);

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

            if (invoice.getRoomId() != null) {
                roomRepository.findById(invoice.getRoomId()).ifPresent(room -> {
                    room.setStatus("Vacant");
                    roomRepository.save(room);
                });
            }

            redirectAttributes.addFlashAttribute("success", "Trả phòng thành công và tất toán mã #" + maHD);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/receptionist/check-out";
    }
}
