package com.votricuong.mayhotel.controllers;

import com.votricuong.mayhotel.documents.Invoice;
import com.votricuong.mayhotel.documents.Room;
import com.votricuong.mayhotel.documents.Service;
import com.votricuong.mayhotel.documents.User;
import com.votricuong.mayhotel.dto.BookingSession;
import com.votricuong.mayhotel.repositories.InvoiceRepository;
import com.votricuong.mayhotel.repositories.RoomRepository;
import com.votricuong.mayhotel.repositories.ServiceRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/booking")
public class BookingController extends BaseController {

    private final RoomRepository roomRepository;
    private final ServiceRepository serviceRepository;
    private final InvoiceRepository invoiceRepository;

    public BookingController(RoomRepository roomRepository, ServiceRepository serviceRepository, InvoiceRepository invoiceRepository) {
        this.roomRepository = roomRepository;
        this.serviceRepository = serviceRepository;
        this.invoiceRepository = invoiceRepository;
    }

    // 1. SELECT SERVICES (Redirect to checkout with services)
    @PostMapping("/select-services")
    public String selectServices(
            @RequestParam(value = "selectedServiceIds", required = false) List<Long> serviceIds,
            HttpSession session) {
        if (serviceIds == null) {
            serviceIds = new ArrayList<>();
        }
        session.setAttribute("selectedServiceIds", serviceIds);
        return "redirect:/booking/checkout";
    }

    // 2. BOOK ROOM VIEW
    @GetMapping("/book")
    public String book(@RequestParam("maLoai") Long maLoai, Model model, RedirectAttributes redirectAttributes) {
        List<Room> allRooms = roomRepository.findAll();
        
        HomeController.RoomTypeDTO loai = allRooms.stream()
            .filter(r -> r.getRoomType() != null && r.getRoomType().getId().equals(maLoai))
            .findFirst()
            .map(r -> HomeController.RoomTypeDTO.builder()
                    .maLoai(r.getRoomType().getId())
                    .name(r.getRoomType().getName())
                    .soNguoi(r.getRoomType().getMaxOccupancy())
                    .price(r.getPrice())
                    .phongs(allRooms.stream().filter(rr -> rr.getRoomType().getId().equals(maLoai)).collect(Collectors.toList()))
                    .build())
            .orElse(null);

        if (loai == null) {
            return "redirect:/error/404";
        }

        long phongTrong = allRooms.stream()
                .filter(p -> p.getRoomType() != null && p.getRoomType().getId().equals(maLoai) && "Vacant".equalsIgnoreCase(p.getStatus()))
                .count();

        if (phongTrong == 0) {
            redirectAttributes.addFlashAttribute("error", "Rất tiếc, loại phòng này hiện tại đã hết phòng trống!");
            return "redirect:/";
        }

        setPageTitle(model, "Đặt phòng: " + loai.getName());
        model.addAttribute("loaiPhong", loai);
        model.addAttribute("soPhongTrong", phongTrong);

        setExtraCSS(model, "view/Booking/booking :: extra_css");
        return render(model, "view/Booking/booking");
    }

    // 3. XỬ LÝ NHẤN "ĐẶT NGAY" -> ĐẨY SANG CHECKOUT
    @PostMapping("/checkout")
    public String startCheckout(@RequestParam("maLoai") Long maLoai,
                            @RequestParam("soLuong") int soLuong,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {

        LocalDate ngayNhan = LocalDate.now();
        LocalDate ngayTra = LocalDate.now().plusDays(1);

        List<Room> allRooms = roomRepository.findAll();
        long phongTrong = allRooms.stream()
                .filter(p -> p.getRoomType() != null && p.getRoomType().getId().equals(maLoai) && "Vacant".equalsIgnoreCase(p.getStatus()))
                .count();

        if (soLuong > phongTrong) {
            redirectAttributes.addFlashAttribute("error", "Chỉ còn " + phongTrong + " phòng trống. Vui lòng chọn lại số lượng.");
            return "redirect:/booking/book?maLoai=" + maLoai;
        }

        HomeController.RoomTypeDTO loai = allRooms.stream()
            .filter(r -> r.getRoomType() != null && r.getRoomType().getId().equals(maLoai))
            .findFirst()
            .map(r -> HomeController.RoomTypeDTO.builder()
                    .maLoai(r.getRoomType().getId())
                    .name(r.getRoomType().getName())
                    .soNguoi(r.getRoomType().getMaxOccupancy())
                    .price(r.getPrice())
                    .phongs(allRooms.stream().filter(rr -> rr.getRoomType().getId().equals(maLoai)).collect(Collectors.toList()))
                    .build())
            .orElse(null);

        if (loai != null) {
            BookingSession currentBooking = new BookingSession();
            currentBooking.setLoaiPhong(loai);
            currentBooking.setSoLuong(soLuong);
            currentBooking.setNgayNhan(ngayNhan);
            currentBooking.setNgayTra(ngayTra);

            session.setAttribute("CurrentBooking", currentBooking);
            
            // Xoá services cũ nếu có
            session.removeAttribute("selectedServiceIds");
            
            return "redirect:/booking/checkout";
        }
        
        return "redirect:/";
    }

    // 4. CHECKOUT VIEW (HIỂN THỊ TRANG THANH TOÁN)
    @GetMapping("/checkout")
    public String checkoutView(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        BookingSession currentBooking = (BookingSession) session.getAttribute("CurrentBooking");
        if (currentBooking == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng chọn phòng để đặt!");
            return "redirect:/";
        }

        List<Long> serviceIds = (List<Long>) session.getAttribute("selectedServiceIds");
        Double totalDichVu = 0.0;
        List<Service> chosenServices = new ArrayList<>();

        if (serviceIds != null && !serviceIds.isEmpty()) {
            for (Long sId : serviceIds) {
                serviceRepository.findById(sId).ifPresent(s -> {
                    chosenServices.add(s);
                });
            }
            totalDichVu = chosenServices.stream().mapToDouble(Service::getPrice).sum();
        }

        setPageTitle(model, "Xác nhận thanh toán đơn đặt");
        model.addAttribute("currentBooking", currentBooking);

        model.addAttribute("chosenServices", chosenServices);
        model.addAttribute("totalDichVu", totalDichVu);
        model.addAttribute("finalTotal", currentBooking.getThanhTien() + totalDichVu);
        
        // Push all services cho trường hợp muốn thêm dịch vụ
        model.addAttribute("listDichVu", serviceRepository.findAll());

        User dummyUser = new User();
        dummyUser.setFullName("");
        dummyUser.setPhone("");
        model.addAttribute("currentUser", dummyUser);

        setExtraCSS(model, "view/Booking/checkout :: extra_css");
        return render(model, "view/Booking/checkout");
    }

    // 5. PROCESS CHECKOUT (XỬ LÝ LƯU INVOICE)
    @PostMapping("/process-checkout")
    public String processCheckout(
            @RequestParam(value = "guestName", required = false) String guestName,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "ngayNhan", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate ngayNhan,
            @RequestParam(value = "ngayTra", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate ngayTra,
            @RequestParam(value = "phuongThucThanhToan", required = false) String phuongThucThanhToan,
            @RequestParam(value = "ghiChu", required = false) String ghiChu,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        BookingSession currentBooking = (BookingSession) session.getAttribute("CurrentBooking");
        if (currentBooking == null) {
            return "redirect:/";
        }

        // Cập nhật ngày nhận/trả từ form Checkout
        if (ngayNhan != null && ngayTra != null && ngayTra.isAfter(ngayNhan)) {
            currentBooking.setNgayNhan(ngayNhan);
            currentBooking.setNgayTra(ngayTra);
        } else if (ngayNhan != null && ngayTra != null && !ngayTra.isAfter(ngayNhan)) {
            redirectAttributes.addFlashAttribute("error", "Ngày trả phòng phải sau ngày nhận phòng!");
            return "redirect:/booking/checkout";
        }

        List<Long> serviceIds = (List<Long>) session.getAttribute("selectedServiceIds");
        Double totalDichVu = 0.0;
        List<Service> chosenServices = new ArrayList<>();
        StringBuilder chuoiDichVu = new StringBuilder();

        if (serviceIds != null && !serviceIds.isEmpty()) {
            for (Long sId : serviceIds) {
                serviceRepository.findById(sId).ifPresent(s -> {
                    chosenServices.add(s);
                    if (chuoiDichVu.length() > 0) chuoiDichVu.append(", ");
                    chuoiDichVu.append(s.getName());
                });
            }
            totalDichVu = chosenServices.stream().mapToDouble(Service::getPrice).sum();
        }

        Long generatedMaHD = null;

        try {
            Long maLoai = currentBooking.getLoaiPhong().getMaLoai();
            int soLuongDat = currentBooking.getSoLuong();

            List<Room> danhSachPhongTrong = roomRepository.findAll().stream()
                    .filter(p -> p.getRoomType() != null && p.getRoomType().getId().equals(maLoai) && "Vacant".equalsIgnoreCase(p.getStatus()))
                    .collect(Collectors.toList());

            if (danhSachPhongTrong.size() < soLuongDat) {
                redirectAttributes.addFlashAttribute("error", "Rất tiếc! Loại phòng này vừa hết phòng trống.");
                return "redirect:/booking/checkout";
            }

            // Gom tất cả vào 1 Invoice duy nhất
            Invoice invoice = new Invoice();
            invoice.setId(System.currentTimeMillis() % 1000000); 
            
            invoice.setGuestName(guestName != null && !guestName.isEmpty() ? guestName : "Guest");
            invoice.setPhone(phone);
            invoice.setBookedAt(new Date());
            
            invoice.setCheckInDate(Date.from(currentBooking.getNgayNhan().atStartOfDay(ZoneId.systemDefault()).toInstant()));
            invoice.setCheckOutDate(Date.from(currentBooking.getNgayTra().atStartOfDay(ZoneId.systemDefault()).toInstant()));
            
            invoice.setInvoiceStatus("Reserved");
            invoice.setPaymentMethod(phuongThucThanhToan);
            invoice.setIsPaid("ONLINE".equalsIgnoreCase(phuongThucThanhToan));
            
            // Tương thích ngược: Lưu ID phòng đầu tiên để view cũ không bị lỗi hiển thị
            if (!danhSachPhongTrong.isEmpty()) {
                invoice.setRoomId(danhSachPhongTrong.get(0).getId());
            }

            List<Invoice.RoomItem> roomItems = new ArrayList<>();
            Double tongGiaCacPhong = 0.0;

            for (int i = 0; i < soLuongDat; i++) {
                Room phongDuocChon = danhSachPhongTrong.get(i);
                
                Invoice.RoomItem rItem = new Invoice.RoomItem(phongDuocChon.getId(), 1, phongDuocChon.getPrice());
                roomItems.add(rItem);
                
                tongGiaCacPhong += phongDuocChon.getPrice() * currentBooking.getSoDem();

                phongDuocChon.setStatus("Reserved");
                roomRepository.save(phongDuocChon);
            }

            invoice.setTotalAmount(tongGiaCacPhong + totalDichVu);

            String noteStr = (ghiChu != null) ? ghiChu.trim() : "";
            if (chuoiDichVu.length() > 0) {
                noteStr += " | [DỊCH VỤ ĐI KÈM]: " + chuoiDichVu.toString();
            }
            invoice.setNote(noteStr);

            invoice.setRoomItems(roomItems);
            
            // Xử lý Service Items
            List<Invoice.ServiceItem> invoiceServiceItems = new ArrayList<>();
            for (Service s : chosenServices) {
                invoiceServiceItems.add(new Invoice.ServiceItem(s.getId(), 1, s.getPrice(), ""));
            }
            invoice.setServiceItems(invoiceServiceItems);
            invoice.setSurchargeItems(new ArrayList<>());

            Invoice savedInvoice = invoiceRepository.save(invoice);
            generatedMaHD = savedInvoice.getId();

            // Xóa session
            session.removeAttribute("CurrentBooking");
            session.removeAttribute("selectedServiceIds");

            if ("ONLINE".equalsIgnoreCase(phuongThucThanhToan)) {
                return "redirect:/booking/payment-gate?id=" + generatedMaHD;
            } else {
                redirectAttributes.addFlashAttribute("success", "Đặt phòng thành công! Quý khách vui lòng thanh toán trực tiếp tại quầy lễ tân.");
                return "redirect:/booking/success";
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi xử lý đặt phòng: " + e.getMessage());
            return "redirect:/booking/checkout";
        }
    }

    // 6. PAYMENT GATE
    @GetMapping("/payment-gate")
    public String showPaymentGate(@RequestParam("id") Long id, Model model, RedirectAttributes ra) {
        Optional<Invoice> invoiceOpt = invoiceRepository.findById(id);
        if (invoiceOpt.isEmpty()) {
            ra.addFlashAttribute("error", "Không tìm thấy thông tin hóa đơn.");
            return "redirect:/";
        }

        setPageTitle(model, "Cổng thanh toán trực tuyến - MAY HOTEL");
        model.addAttribute("invoice", invoiceOpt.get());
        return render(model, "view/Booking/payment_gate");
    }

    // 7. COMPLETE PAYMENT
    @PostMapping("/complete-online-payment")
    public String completeOnlinePayment(@RequestParam("maHD") Long maHD, RedirectAttributes ra) {
        try {
            Optional<Invoice> invoiceOpt = invoiceRepository.findById(maHD);
            if (invoiceOpt.isPresent()) {
                Invoice invoice = invoiceOpt.get();
                invoice.setIsPaid(true);
                invoice.setNote((invoice.getNote() != null ? invoice.getNote() : "") + " | [ONLINE] Đã thanh toán trực tuyến thành công.");
                invoiceRepository.save(invoice);
                ra.addFlashAttribute("success", "Thanh toán trực tuyến thành công! Phòng của bạn đã được đảm bảo.");
            }
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi kết toán giao dịch trực tuyến: " + e.getMessage());
        }
        return "redirect:/booking/success";
    }

    // 8. SUCCESS
    @GetMapping("/success")
    public String success(Model model) {
        setPageTitle(model, "Đặt phòng thành công");
        setExtraCSS(model, "view/Booking/success :: extra_css");
        return render(model, "view/Booking/success");
    }

    // 9. HISTORY
    @GetMapping("/history")
    public String bookingHistory(@RequestParam(value="phone", required=false) String phone, Model model, RedirectAttributes redirectAttributes) {
        if (phone == null || phone.isEmpty()) {
            model.addAttribute("historyList", new ArrayList<>());
        } else {
            List<Invoice> historyList = invoiceRepository.findByPhone(phone);
            model.addAttribute("historyList", historyList);
        }
        
        setPageTitle(model, "Lịch sử đặt phòng");
        setExtraCSS(model, "view/Booking/history :: extra_css");
        return render(model, "view/Booking/history");
    }

    // 10. HISTORY DETAIL
    @GetMapping("/history/detail")
    public String bookingHistoryDetail(@RequestParam("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Invoice> invoiceOpt = invoiceRepository.findById(id);
        if (invoiceOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy hóa đơn mã số này!");
            return "redirect:/booking/history";
        }

        Invoice invoice = invoiceOpt.get();
        
        if (invoice.getRoomId() != null) {
            roomRepository.findById(invoice.getRoomId()).ifPresent(room -> {
                model.addAttribute("phong", room);
            });
        }

        setPageTitle(model, "Chi tiết đơn đặt phòng #" + invoice.getId());
        model.addAttribute("hoaDon", invoice);

        setExtraCSS(model, "view/Booking/historydetail :: extra_css");
        return render(model, "view/Booking/historydetail");
    }
}
