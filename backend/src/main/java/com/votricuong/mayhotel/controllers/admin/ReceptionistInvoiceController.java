package com.votricuong.mayhotel.controllers.admin;

import com.votricuong.mayhotel.controllers.BaseController;
import com.votricuong.mayhotel.documents.BookingDetail;
import com.votricuong.mayhotel.documents.BookingOrder;
import com.votricuong.mayhotel.documents.Customer;
import com.votricuong.mayhotel.documents.Invoice;
import com.votricuong.mayhotel.documents.Room;
import com.votricuong.mayhotel.repositories.BookingDetailRepository;
import com.votricuong.mayhotel.repositories.BookingOrderRepository;
import com.votricuong.mayhotel.repositories.CustomerRepository;
import com.votricuong.mayhotel.repositories.InvoiceRepository;
import com.votricuong.mayhotel.repositories.RoomRepository;
import com.votricuong.mayhotel.services.SequenceGeneratorService;
import lombok.Builder;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping({"/receptionist/invoices", "/receptionist/invoice"})
public class ReceptionistInvoiceController extends BaseController {

    private final InvoiceRepository invoiceRepository;
    private final RoomRepository roomRepository;
    private final CustomerRepository customerRepository;
    private final BookingOrderRepository bookingOrderRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    public ReceptionistInvoiceController(InvoiceRepository invoiceRepository,
                                         RoomRepository roomRepository,
                                         CustomerRepository customerRepository,
                                         BookingOrderRepository bookingOrderRepository,
                                         BookingDetailRepository bookingDetailRepository,
                                         SequenceGeneratorService sequenceGeneratorService) {
        this.invoiceRepository = invoiceRepository;
        this.roomRepository = roomRepository;
        this.customerRepository = customerRepository;
        this.bookingOrderRepository = bookingOrderRepository;
        this.bookingDetailRepository = bookingDetailRepository;
        this.sequenceGeneratorService = sequenceGeneratorService;
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

    // 4. POST /create: Tạo đơn đặt phòng khách lẻ (LapHoaDon)
    @PostMapping("/create")
    public String createInvoice(@RequestParam("guestName") String guestName,
                                @RequestParam("phone") String phone,
                                @RequestParam("checkInDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
                                @RequestParam("checkOutDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate,
                                @RequestParam(value = "roomId", required = false) Long roomId,
                                @RequestParam(value = "note", required = false) String note,
                                RedirectAttributes redirectAttributes) {
        try {
            Long nextId = sequenceGeneratorService.generateSequence(Invoice.SEQUENCE_NAME);
            
            Date checkIn = Date.from(checkInDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            Date checkOut = Date.from(checkOutDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

            long numNights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
            if (numNights <= 0) numNights = 1;

            Double pricePerNight = 500000.0;
            if (roomId != null) {
                Optional<Room> roomOpt = roomRepository.findById(roomId);
                if (roomOpt.isPresent() && roomOpt.get().getPrice() != null) {
                    pricePerNight = roomOpt.get().getPrice();
                }
            }

            Double totalAmount = pricePerNight * numNights;

            Invoice invoice = Invoice.builder()
                    .id(nextId)
                    .guestName(guestName)
                    .phone(phone)
                    .roomId(roomId)
                    .checkInDate(checkIn)
                    .checkOutDate(checkOut)
                    .totalAmount(totalAmount)
                    .invoiceStatus("Reserved")
                    .payMethod("Tiền mặt")
                    .isPaid(false)
                    .createdAt(new Date())
                    .note(note)
                    .build();

            invoiceRepository.save(invoice);

            // Ghi nhận BookingOrder tương ứng
            Long nextBookingId = sequenceGeneratorService.generateSequence("booking_orders");
            BookingOrder booking = BookingOrder.builder()
                    .id(nextBookingId)
                    .orderDate(new Date())
                    .checkInDate(checkIn)
                    .checkOutDate(checkOut)
                    .totalAmount(totalAmount)
                    .status("Reserved")
                    .paymentStatus("UNPAID")
                    .depositAmount(0.0)
                    .noShowRiskLevel("Thấp")
                    .build();
            bookingOrderRepository.save(booking);

            invoice.setBookingId(nextBookingId);
            invoiceRepository.save(invoice);

            redirectAttributes.addFlashAttribute("success", "Tạo thành công Hóa đơn #" + nextId + " cho khách " + guestName);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi tạo hóa đơn: " + e.getMessage());
        }
        return "redirect:/receptionist/invoices";
    }

    // 5. GET /delete/{id}: Xóa hóa đơn
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
