package com.votricuong.mayhotel.controllers.admin;

import com.votricuong.mayhotel.controllers.BaseController;
import com.votricuong.mayhotel.documents.BookingDetail;
import com.votricuong.mayhotel.documents.Invoice;
import com.votricuong.mayhotel.documents.Room;
import com.votricuong.mayhotel.repositories.BookingDetailRepository;
import com.votricuong.mayhotel.repositories.InvoiceRepository;
import com.votricuong.mayhotel.repositories.RoomRepository;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping({"/receptionist/invoices", "/receptionist/invoice"})
public class ReceptionistInvoiceController extends BaseController {

    private final InvoiceRepository invoiceRepository;
    private final RoomRepository roomRepository;
    private final BookingDetailRepository bookingDetailRepository;

    public ReceptionistInvoiceController(InvoiceRepository invoiceRepository,
                                         RoomRepository roomRepository,
                                         BookingDetailRepository bookingDetailRepository) {
        this.invoiceRepository = invoiceRepository;
        this.roomRepository = roomRepository;
        this.bookingDetailRepository = bookingDetailRepository;
    }

    @Data
    @Builder
    public static class FullInvoiceDTO {
        private Long id;
        private Long maBooking;
        private String hoTen;
        private String sdt;
        private String diaChi;
        private Long maPhong;
        private String tenPhong;
        private Date ngayDat;
        private Date ngayCheckIn;
        private Date ngayCheckOut;
        private Double totalPrice;
        private String payMethod;
        private String invoiceStatus;
        private Boolean isPaid;
        private Room phong;
        private Long soDem;
    }

    private FullInvoiceDTO mapToFullDTO(Invoice inv) {
        String tenPhongStr = "";
        Room rObj = null;
        if (inv.getRoomId() != null) {
            rObj = roomRepository.findById(inv.getRoomId()).orElse(null);
            if (rObj != null) {
                tenPhongStr = rObj.getName();
            }
        } else if (inv.getBookingId() != null) {
            List<BookingDetail> details = bookingDetailRepository.findByBookingId(inv.getBookingId());
            if (details != null && !details.isEmpty() && details.get(0).getRoomId() != null) {
                rObj = roomRepository.findById(details.get(0).getRoomId()).orElse(null);
                if (rObj != null) {
                    tenPhongStr = rObj.getName();
                }
            }
        }

        // Tính số đêm
        long soDem = 1;
        if (inv.getCheckInDate() != null && inv.getCheckOutDate() != null) {
            LocalDate d1 = inv.getCheckInDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate d2 = inv.getCheckOutDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            soDem = ChronoUnit.DAYS.between(d1, d2);
            if (soDem <= 0) soDem = 1;
        }

        return FullInvoiceDTO.builder()
                .id(inv.getId())
                .maBooking(inv.getBookingId())
                .hoTen(inv.getGuestName() != null ? inv.getGuestName() : "Khách hàng lẻ")
                .sdt(inv.getPhone() != null ? inv.getPhone() : "Chưa có SĐT")
                .diaChi("Việt Nam")
                .maPhong(inv.getRoomId())
                .tenPhong(!tenPhongStr.isEmpty() ? tenPhongStr : (inv.getRoomId() != null ? "Phòng " + inv.getRoomId() : "Chưa chọn phòng"))
                .ngayDat(inv.getCreatedAt() != null ? inv.getCreatedAt() : new Date())
                .ngayCheckIn(inv.getCheckInDate())
                .ngayCheckOut(inv.getCheckOutDate())
                .totalPrice(inv.getTotalAmount() != null ? inv.getTotalAmount() : 0.0)
                .payMethod(inv.getPayMethod() != null ? inv.getPayMethod() : "Tiền mặt")
                .invoiceStatus(inv.getInvoiceStatus() != null ? inv.getInvoiceStatus() : "Pending")
                .isPaid(Boolean.TRUE.equals(inv.getIsPaid()))
                .phong(rObj)
                .soDem(soDem)
                .build();
    }

    // 1. GET /: Hiển thị danh sách hóa đơn (TruyVanDanhSach / HienThiDanhSach)
    @GetMapping
    public String index(@RequestParam(value = "keyword", required = false) String keyword,
                        @RequestParam(value = "status", required = false) String status,
                        Model model) {
        setPageTitle(model, "Quản lý Hóa đơn - Lễ tân");

        List<Invoice> invoices = invoiceRepository.findAll();

        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.toLowerCase().trim();
            invoices = invoices.stream().filter(inv -> 
                (inv.getId() != null && inv.getId().toString().contains(kw)) ||
                (inv.getGuestName() != null && inv.getGuestName().toLowerCase().contains(kw)) ||
                (inv.getPhone() != null && inv.getPhone().contains(kw))
            ).collect(Collectors.toList());
        }

        if (status != null && !status.trim().isEmpty()) {
            invoices = invoices.stream().filter(inv ->
                status.equalsIgnoreCase(inv.getInvoiceStatus())
            ).collect(Collectors.toList());
        }

        List<FullInvoiceDTO> dtoList = invoices.stream()
                .map(this::mapToFullDTO)
                .collect(Collectors.toList());

        model.addAttribute("invoices", dtoList);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);

        setExtraCSS(model, "view/Admin/Invoice/index :: extra_css");
        setExtraJS(model, "view/Admin/Invoice/index :: extra_js");
        return render(model, "view/Admin/Invoice/index");
    }

    // 2. GET /details/{id}: Xem chi tiết hóa đơn (TruyXuatChiTiet / HienThiChiTietHoaDon)
    @GetMapping("/details/{id}")
    public String details(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        setPageTitle(model, "Chi tiết Hóa đơn #" + id);

        Optional<Invoice> invoiceOpt = invoiceRepository.findById(id);
        if (invoiceOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy hóa đơn #" + id);
            return "redirect:/receptionist/invoices";
        }

        FullInvoiceDTO dto = mapToFullDTO(invoiceOpt.get());
        model.addAttribute("invoice", dto);

        setExtraCSS(model, "view/Admin/Invoice/details :: extra_css");
        setExtraJS(model, "view/Admin/Invoice/details :: extra_js");
        return render(model, "view/Admin/Invoice/details");
    }

    // 3. GET /print/{id}: In hóa đơn GTGT
    @GetMapping("/print/{id}")
    public String printInvoice(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Invoice> invoiceOpt = invoiceRepository.findById(id);
        if (invoiceOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy hóa đơn #" + id);
            return "redirect:/receptionist/invoices";
        }

        FullInvoiceDTO dto = mapToFullDTO(invoiceOpt.get());
        model.addAttribute("invoice", dto);
        model.addAttribute("soDem", dto.getSoDem());

        return "view/Admin/Invoice/print";
    }

    // 4. GET /delete/{id}: Xóa hóa đơn
    @GetMapping("/delete/{id}")
    public String deleteInvoice(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            if (invoiceRepository.existsById(id)) {
                invoiceRepository.deleteById(id);
                redirectAttributes.addFlashAttribute("success", "Đã xóa hóa đơn #" + id);
            } else {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy hóa đơn #" + id);
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không thể xóa hóa đơn: " + e.getMessage());
        }
        return "redirect:/receptionist/invoices";
    }
}
