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
    private final com.votricuong.mayhotel.repositories.BookingOrderRepository bookingOrderRepository;
    private final com.votricuong.mayhotel.repositories.BookingDetailRepository bookingDetailRepository;
    private final com.votricuong.mayhotel.repositories.ServiceTicketRepository serviceTicketRepository;
    private final com.votricuong.mayhotel.repositories.CustomerRepository customerRepository;
    private final com.votricuong.mayhotel.services.SequenceGeneratorService sequenceGeneratorService;
    private final InvoiceRepository invoiceRepository;
    private final com.votricuong.mayhotel.repositories.RoomTypeRepository roomTypeRepository;
    private final com.votricuong.mayhotel.services.LoyaltyService loyaltyService;
    private final com.votricuong.mayhotel.services.ReviewService reviewService;
    private final com.votricuong.mayhotel.services.NoShowPredictionService noShowPredictionService;
    private final com.votricuong.mayhotel.services.BookingCancellationService bookingCancellationService;
    private final com.votricuong.mayhotel.services.BookingAvailabilityService bookingAvailabilityService;

    public BookingController(RoomRepository roomRepository, 
                             ServiceRepository serviceRepository, 
                             com.votricuong.mayhotel.repositories.BookingOrderRepository bookingOrderRepository,
                             com.votricuong.mayhotel.repositories.BookingDetailRepository bookingDetailRepository,
                             com.votricuong.mayhotel.repositories.ServiceTicketRepository serviceTicketRepository,
                             com.votricuong.mayhotel.repositories.CustomerRepository customerRepository,
                             com.votricuong.mayhotel.services.SequenceGeneratorService sequenceGeneratorService,
                             InvoiceRepository invoiceRepository,
                             com.votricuong.mayhotel.repositories.RoomTypeRepository roomTypeRepository,
                             com.votricuong.mayhotel.services.LoyaltyService loyaltyService,
                             com.votricuong.mayhotel.services.ReviewService reviewService,
                             com.votricuong.mayhotel.services.NoShowPredictionService noShowPredictionService,
                             com.votricuong.mayhotel.services.BookingCancellationService bookingCancellationService,
                             com.votricuong.mayhotel.services.BookingAvailabilityService bookingAvailabilityService) {
        this.roomRepository = roomRepository;
        this.serviceRepository = serviceRepository;
        this.bookingOrderRepository = bookingOrderRepository;
        this.bookingDetailRepository = bookingDetailRepository;
        this.serviceTicketRepository = serviceTicketRepository;
        this.customerRepository = customerRepository;
        this.sequenceGeneratorService = sequenceGeneratorService;
        this.invoiceRepository = invoiceRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.loyaltyService = loyaltyService;
        this.reviewService = reviewService;
        this.noShowPredictionService = noShowPredictionService;
        this.bookingCancellationService = bookingCancellationService;
        this.bookingAvailabilityService = bookingAvailabilityService;
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
    public String book(@RequestParam("maLoai") Long maLoai, Model model, RedirectAttributes redirectAttributes, HttpSession session, jakarta.servlet.http.HttpServletRequest request) {
        if (session.getAttribute("user") == null) {
            return "redirect:/account/login?error=auth";
        }
        
        List<Room> allRooms = roomRepository.findAll();
        
        com.votricuong.mayhotel.documents.RoomType type = roomTypeRepository.findById(maLoai).orElse(null);
        
        HomeController.RoomTypeDTO loai = allRooms.stream()
            .filter(r -> r.getRoomTypeId() != null && r.getRoomTypeId().equals(maLoai))
            .findFirst()
            .map(r -> toBookingRoomType(r, type, allRooms))
            .orElse(null);

        if (loai == null) {
            return "redirect:/error/404";
        }

        long phongTrong = allRooms.stream()
                .filter(p -> p.getRoomTypeId() != null && p.getRoomTypeId().equals(maLoai) && 
                            ("Vacant".equalsIgnoreCase(p.getStatus()) || 
                             "Còn phòng".equalsIgnoreCase(p.getStatus()) || 
                             "Trống".equalsIgnoreCase(p.getStatus()) || 
                             "Phòng trống".equalsIgnoreCase(p.getStatus())))
                .count();

        if (phongTrong == 0) {
            redirectAttributes.addFlashAttribute("error", "Rất tiếc, loại phòng này hiện tại đã hết phòng trống!");
            String referer = request.getHeader("Referer");
            return "redirect:" + (referer != null ? referer : "/rooms");
        }

        setPageTitle(model, "Đặt phòng: " + loai.getName());
        model.addAttribute("loaiPhong", loai);
        model.addAttribute("soPhongTrong", phongTrong);

        setExtraCSS(model, "view/Booking/booking :: extra_css");
        return "view/Booking/booking";
    }

    // 3. XỬ LÝ NHẤN "ĐẶT NGAY" -> ĐẨY SANG CHECKOUT
    @PostMapping("/checkout")
    public String startCheckout(@RequestParam("maLoai") Long maLoai,
                            @RequestParam("soLuong") int soLuong,
                            HttpSession session,
                            RedirectAttributes redirectAttributes,
                            jakarta.servlet.http.HttpServletRequest request) {

        LocalDate ngayNhan = LocalDate.now();
        LocalDate ngayTra = LocalDate.now().plusDays(1);

        List<Room> allRooms = roomRepository.findAll();
        long phongTrong = allRooms.stream()
                .filter(p -> p.getRoomTypeId() != null && p.getRoomTypeId().equals(maLoai) && 
                            ("Vacant".equalsIgnoreCase(p.getStatus()) || 
                             "Còn phòng".equalsIgnoreCase(p.getStatus()) || 
                             "Trống".equalsIgnoreCase(p.getStatus()) || 
                             "Phòng trống".equalsIgnoreCase(p.getStatus())))
                .count();

        if (soLuong > phongTrong) {
            redirectAttributes.addFlashAttribute("error", "Chỉ còn " + phongTrong + " phòng trống. Vui lòng chọn lại số lượng.");
            return "redirect:/booking/book?maLoai=" + maLoai;
        }

        com.votricuong.mayhotel.documents.RoomType type = roomTypeRepository.findById(maLoai).orElse(null);

        HomeController.RoomTypeDTO loai = allRooms.stream()
            .filter(r -> r.getRoomTypeId() != null && r.getRoomTypeId().equals(maLoai))
            .findFirst()
            .map(r -> toBookingRoomType(r, type, allRooms))
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
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/rooms");
    }

    // 3.5. XỬ LÝ NHẤN "ĐẶT NGAY" TỪ TRANG CHI TIẾT (detail.html)
    @GetMapping("/create")
    public String createBookingFromDetail(@RequestParam("roomId") Long roomId,
                                          @RequestParam("soDem") int soDem,
                                          HttpSession session,
                                          RedirectAttributes redirectAttributes) {

        if (session.getAttribute("user") == null) {
            return "redirect:/account/login?error=auth";
        }

        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy phòng hợp lệ!");
            return "redirect:/rooms";
        }

        Room selectedRoom = roomOpt.get();
        Long maLoai = selectedRoom.getRoomTypeId();
        
        List<Room> allRooms = roomRepository.findAll();
        long phongTrong = allRooms.stream()
                .filter(p -> p.getRoomTypeId() != null && p.getRoomTypeId().equals(maLoai) && 
                       ("Vacant".equalsIgnoreCase(p.getStatus()) || 
                        "Còn phòng".equalsIgnoreCase(p.getStatus()) || 
                        "Trống".equalsIgnoreCase(p.getStatus()) || 
                        "Phòng trống".equalsIgnoreCase(p.getStatus())))
                .count();

        if (phongTrong < 1) {
            redirectAttributes.addFlashAttribute("error", "Rất tiếc, loại phòng này hiện tại đã hết phòng trống!");
            return "redirect:/rooms/detail/" + maLoai;
        }

        LocalDate ngayNhan = LocalDate.now();
        LocalDate ngayTra = LocalDate.now().plusDays(soDem > 0 ? soDem : 1);

        com.votricuong.mayhotel.documents.RoomType type = roomTypeRepository.findById(maLoai).orElse(null);

        HomeController.RoomTypeDTO loai = allRooms.stream()
            .filter(r -> r.getRoomTypeId() != null && r.getRoomTypeId().equals(maLoai))
            .findFirst()
            .map(r -> toBookingRoomType(r, type, allRooms))
            .orElse(null);

        if (loai != null) {
            BookingSession currentBooking = new BookingSession();
            currentBooking.setLoaiPhong(loai);
            currentBooking.setSoLuong(1); // Đặt 1 phòng từ trang chi tiết
            currentBooking.setNgayNhan(ngayNhan);
            currentBooking.setNgayTra(ngayTra);

            session.setAttribute("CurrentBooking", currentBooking);
            session.removeAttribute("selectedServiceIds");
            return "redirect:/booking/checkout";
        }

        return "redirect:/error/404";
    }

    // 4. CHECKOUT VIEW (HIỂN THỊ TRANG THANH TOÁN)
    @GetMapping("/checkout")
    public String checkoutView(
            @RequestParam(value = "maLoai", required = false) String maLoaiParam,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes,
            jakarta.servlet.http.HttpServletRequest request) {
        if (maLoaiParam != null && !maLoaiParam.isBlank()) {
            Long maLoai = parseRoomTypeId(maLoaiParam);
            if (maLoai == null) {
                redirectAttributes.addFlashAttribute("error", "Mã loại phòng không hợp lệ.");
                return "redirect:/";
            }

            String bookingResult = startCheckout(
                    maLoai,
                    1,
                    session,
                    redirectAttributes,
                    request);
            if (!"redirect:/booking/checkout".equals(bookingResult)) {
                return bookingResult;
            }
        }

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
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser != null) {
            dummyUser = sessionUser;
        }
        int availablePoints = sessionUser == null
                ? 0
                : customerRepository.findFirstByUserIdOrderByIdAsc(sessionUser.getId())
                        .map(customer -> customer.getPoints() == null ? 0 : customer.getPoints())
                        .orElse(0);
        model.addAttribute("currentUser", dummyUser);
        model.addAttribute("availablePoints", availablePoints);

        setExtraCSS(model, "view/Booking/checkout :: extra_css");
        return "view/Booking/checkout";
    }

    private Long parseRoomTypeId(String value) {
        String normalized = value.trim();
        if (normalized.matches("(?i)L\\d+")) {
            normalized = normalized.substring(1);
        }
        try {
            return Long.valueOf(normalized);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    // 5. PROCESS CHECKOUT (XỬ LÝ LƯU BOOKING ORDER)
    @PostMapping("/process-checkout")
    public String processCheckout(
            @RequestParam(value = "guestName", required = false) String guestName,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "ngayNhan", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate ngayNhan,
            @RequestParam(value = "ngayTra", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate ngayTra,
            @RequestParam(value = "phuongThucThanhToan", required = false) String phuongThucThanhToan,
            @RequestParam(value = "ghiChu", required = false) String ghiChu,
            @RequestParam(value = "diemDoi", defaultValue = "0") Integer diemDoi,
            @RequestParam(value = "giamGiaDiem", defaultValue = "0") Double giamGiaDiem,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        BookingSession currentBooking = (BookingSession) session.getAttribute("CurrentBooking");
        if (currentBooking == null) {
            return "redirect:/";
        }

        try {
            if (ngayNhan != null || ngayTra != null) {
                com.votricuong.mayhotel.utils.InputValidator.stay(ngayNhan, ngayTra);
                currentBooking.setNgayNhan(ngayNhan);
                currentBooking.setNgayTra(ngayTra);
            }
            com.votricuong.mayhotel.utils.InputValidator.stay(currentBooking.getNgayNhan(), currentBooking.getNgayTra());
            guestName = com.votricuong.mayhotel.utils.InputValidator.requiredText(guestName, "Họ và tên", 100);
            phone = com.votricuong.mayhotel.utils.InputValidator.phone(phone);
            ghiChu = com.votricuong.mayhotel.utils.InputValidator.optionalText(ghiChu, "Ghi chú", 500);
            phuongThucThanhToan = com.votricuong.mayhotel.utils.InputValidator.oneOf(
                    phuongThucThanhToan, "Phương thức thanh toán", java.util.Set.of("COD", "ONLINE"));
            com.votricuong.mayhotel.utils.InputValidator.range(currentBooking.getSoLuong(), "Số lượng phòng", 1, 10);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
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

            Date requestedIn = Date.from(currentBooking.getNgayNhan().atStartOfDay(ZoneId.systemDefault()).toInstant());
            Date requestedOut = Date.from(currentBooking.getNgayTra().atStartOfDay(ZoneId.systemDefault()).toInstant());
            List<Room> danhSachPhongTrong = bookingAvailabilityService.findAvailableRooms(
                    maLoai, requestedIn, requestedOut);

            if (danhSachPhongTrong.size() < soLuongDat) {
                redirectAttributes.addFlashAttribute("error", "Ngày bạn chọn đã hết phòng! Vui lòng đổi ngày khác.");
                return "redirect:/booking/checkout";
            }
            
            User sessionUser = (User) session.getAttribute("user");
            // Tài khoản đăng nhập là nguồn liên kết chính; chỉ dùng số điện thoại
            // để tìm khách vãng lai khi không có bản ghi theo user_id.
            com.votricuong.mayhotel.documents.Customer customer = sessionUser == null
                    ? null
                    : customerRepository.findFirstByUserIdOrderByIdAsc(sessionUser.getId()).orElse(null);
            if (customer == null && phone != null && !phone.isEmpty()) {
                List<com.votricuong.mayhotel.documents.Customer> existingCustomers = customerRepository.findByPhone(phone);
                if (!existingCustomers.isEmpty()) {
                    customer = existingCustomers.get(0);
                }
            }

            if (customer == null) {
                customer = new com.votricuong.mayhotel.documents.Customer();
                customer.setId(sequenceGeneratorService.generateSequence("customers_sequence"));
                customer.setFullName(guestName != null && !guestName.isEmpty() ? guestName : "Guest");
                customer.setPhone(phone);
                if (sessionUser != null) {
                    customer.setUserId(sessionUser.getId());
                    customer.setEmail(sessionUser.getEmail());
                }
                customerRepository.save(customer);
            } else {
                // Update missing info for existing customer
                boolean needUpdate = false;
                if (sessionUser != null && customer.getUserId() == null) {
                    customer.setUserId(sessionUser.getId());
                    if (customer.getEmail() == null || customer.getEmail().isEmpty()) {
                        customer.setEmail(sessionUser.getEmail());
                    }
                    needUpdate = true;
                }
                if (needUpdate) {
                    customerRepository.save(customer);
                }
            }

            int requestedPoints = diemDoi == null ? 0 : diemDoi;
            int availablePoints = customer.getPoints() == null ? 0 : customer.getPoints();
            if (requestedPoints < 0 || requestedPoints > availablePoints) {
                redirectAttributes.addFlashAttribute("error", "Số điểm sử dụng không hợp lệ hoặc vượt quá điểm hiện có.");
                return "redirect:/booking/checkout";
            }

            // Tạo BookingOrder
            com.votricuong.mayhotel.documents.BookingOrder bookingOrder = new com.votricuong.mayhotel.documents.BookingOrder();
            bookingOrder.setId(sequenceGeneratorService.generateSequence("booking_orders_sequence")); 
            bookingOrder.setCustomerId(customer.getId());
            bookingOrder.setBookingDate(new Date());
            bookingOrder.setExpectedIn(Date.from(currentBooking.getNgayNhan().atStartOfDay(ZoneId.systemDefault()).toInstant()));
            bookingOrder.setExpectedOut(Date.from(currentBooking.getNgayTra().atStartOfDay(ZoneId.systemDefault()).toInstant()));
            bookingOrder.setStatus("Pending");
            bookingOrder.setBookingSource("WEB");
            bookingOrder.setRiskAnalysisStatus("PENDING");
            
            String noteStr = (ghiChu != null) ? ghiChu.trim() : "";
            bookingOrder.setNotes(noteStr);

            Double tongGiaCacPhong = 0.0;
            
            com.votricuong.mayhotel.documents.BookingOrder savedBookingOrder = bookingOrderRepository.save(bookingOrder);
            generatedMaHD = savedBookingOrder.getId();

            for (int i = 0; i < soLuongDat; i++) {
                Room phongDuocChon = danhSachPhongTrong.get(i);
                
                com.votricuong.mayhotel.documents.BookingDetail bd = new com.votricuong.mayhotel.documents.BookingDetail();
                bd.setId(sequenceGeneratorService.generateSequence("booking_details_sequence"));
                bd.setBookingId(savedBookingOrder.getId());
                bd.setRoomId(phongDuocChon.getId());
                bd.setUnitPrice(phongDuocChon.getPrice());
                bookingDetailRepository.save(bd);
                
                tongGiaCacPhong += phongDuocChon.getPrice() * currentBooking.getSoDem();

                phongDuocChon.setStatus("Reserved");
                roomRepository.save(phongDuocChon);
            }
            
            // Xử lý Service Items
            for (Service s : chosenServices) {
                com.votricuong.mayhotel.documents.ServiceTicket st = new com.votricuong.mayhotel.documents.ServiceTicket();
                st.setId(sequenceGeneratorService.generateSequence("service_tickets_sequence"));
                st.setBookingId(savedBookingOrder.getId());
                st.setServiceId(s.getId());
                st.setQuantity(1);
                st.setStatus("PENDING");
                // st.setPrice(s.getPrice());
                // st.setOrderDate(new Date());
                // st.setStatus("Delivered");
                serviceTicketRepository.save(st);
            }

            double roomAndServiceTotal = tongGiaCacPhong + totalDichVu;
            double pointsDiscount = Math.min(requestedPoints * 1000.0, roomAndServiceTotal);
            int redeemedPoints = (int) Math.ceil(pointsDiscount / 1000.0);
            if (redeemedPoints > 0) {
                redeemedPoints = loyaltyService.redeemPoints(customer, redeemedPoints);
            }

            // Xóa session
            session.removeAttribute("CurrentBooking");
            session.removeAttribute("selectedServiceIds");

            // TẠO INVOICE ĐỂ LỄ TÂN CÓ THỂ THẤY Ở MÀN HÌNH CHECK-IN
            Invoice invoice = new Invoice();
            invoice.setId(sequenceGeneratorService.generateSequence("invoices_sequence"));
            invoice.setBookingId(generatedMaHD);
            invoice.setCreatedAt(new Date());
            invoice.setTotalAmount(roomAndServiceTotal - pointsDiscount);
            invoice.setPayMethod(phuongThucThanhToan);
            invoice.setInvoiceStatus("Reserved");
            invoice.setNote(noteStr);
            invoice.setGuestName(customer.getFullName());
            invoice.setPhone(customer.getPhone());
            // KHÔNG GÁN roomId Ở ĐÂY, ĐỂ CHO BÊN LỄ TÂN GÁN (trạng thái Chưa xếp phòng)
            // invoice.setRoomId(danhSachPhongTrong.get(0).getId());
            invoice.setCheckInDate(bookingOrder.getExpectedIn());
            invoice.setCheckOutDate(bookingOrder.getExpectedOut());
            invoice.setUserId(customer.getUserId());
            invoice.setIsPaid(false);
            invoice.setSurcharge(0.0);
            invoice.setRedeemedPoints(redeemedPoints);
            invoice.setPointsDiscount(pointsDiscount);
            invoiceRepository.save(invoice);
            noShowPredictionService.analyzeAndSave(savedBookingOrder.getId());

            if ("ONLINE".equalsIgnoreCase(phuongThucThanhToan)) {
                return "redirect:/booking/payment-gate?id=" + generatedMaHD;
            } else {
                redirectAttributes.addFlashAttribute("success", "Đặt phòng thành công! Quý khách vui lòng thanh toán trực tiếp tại quầy lễ tân.");
                redirectAttributes.addFlashAttribute("successBookingId", savedBookingOrder.getId());
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
        return "view/Booking/payment";
    }

    // 7. COMPLETE PAYMENT
    @PostMapping("/complete-online-payment")
    public String completeOnlinePayment(@RequestParam("maHD") Long maHD, RedirectAttributes ra) {
        try {
            Optional<Invoice> invoiceOpt = invoiceRepository.findById(maHD);
            if (invoiceOpt.isPresent()) {
                Invoice invoice = invoiceOpt.get();
                invoice.setIsPaid(true);
                invoice.setInvoiceStatus("Paid");
                invoice.setNote((invoice.getNote() != null ? invoice.getNote() : "") + " | [ONLINE] Đã thanh toán trực tuyến thành công.");
                invoiceRepository.save(invoice);
                loyaltyService.awardPointsForPaidInvoice(invoice);
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

        Object rawId = model.asMap().get("successBookingId");
        Long bookingId = null;
        if (rawId instanceof Number) {
            bookingId = ((Number) rawId).longValue();
        } else if (rawId != null) {
            try {
                bookingId = Long.parseLong(rawId.toString());
            } catch (NumberFormatException ignored) {
                // The success page still has a safe fallback without order details.
            }
        }

        if (bookingId != null) {
            final Long resolvedBookingId = bookingId;
            bookingOrderRepository.findById(resolvedBookingId).ifPresent(order -> {
                model.addAttribute("order", order);
                model.addAttribute("maDatPhong", order.getId());

                List<Invoice> invoices = invoiceRepository.findByBookingId(resolvedBookingId);
                model.addAttribute("hoaDon", invoices != null && !invoices.isEmpty() ? invoices.get(0) : null);

                List<com.votricuong.mayhotel.documents.BookingDetail> details =
                        bookingDetailRepository.findByBookingId(resolvedBookingId);
                if (details != null) {
                    details.stream().filter(detail -> detail.getRoomId() != null).findFirst().ifPresent(detail ->
                        roomRepository.findById(detail.getRoomId()).ifPresent(room -> {
                            model.addAttribute("phong", room);
                            if (room.getRoomTypeId() != null) {
                                roomTypeRepository.findById(room.getRoomTypeId())
                                        .ifPresent(type -> model.addAttribute("loaiPhong", type));
                            }
                        })
                    );
                }

                model.addAttribute("soDem", calculateNights(order));
                customerRepository.findById(order.getCustomerId())
                        .ifPresent(customer -> model.addAttribute("customer", customer));
            });
        }
        return "view/Booking/success";
    }

    // 9. HISTORY
    @GetMapping("/history")
    public String bookingHistory(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        com.votricuong.mayhotel.documents.User user = (com.votricuong.mayhotel.documents.User) session.getAttribute("user");
        
        List<com.votricuong.mayhotel.documents.BookingOrder> historyList = new ArrayList<>();

        if (user != null) {
            // Lấy tất cả khách hàng khớp với User (theo ID, số điện thoại, hoặc email)
            List<com.votricuong.mayhotel.documents.Customer> matchedCustomers = new ArrayList<>();
            
            // 1. Tìm theo UserId (Bây giờ trả về List thay vì Optional)
            matchedCustomers.addAll(customerRepository.findByUserId(user.getId()));
            
            // 2. Tìm theo Phone
            if (user.getPhone() != null && !user.getPhone().isEmpty()) {
                customerRepository.findByPhone(user.getPhone()).forEach(c -> {
                    if (matchedCustomers.stream().noneMatch(mc -> mc.getId().equals(c.getId()))) {
                        matchedCustomers.add(c);
                    }
                });
            }
            
            // 3. Tìm theo Email
            if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                customerRepository.findByEmail(user.getEmail()).forEach(c -> {
                    if (matchedCustomers.stream().noneMatch(mc -> mc.getId().equals(c.getId()))) {
                        matchedCustomers.add(c);
                    }
                });
            }

            if (!matchedCustomers.isEmpty()) {
                List<Long> customerIds = matchedCustomers.stream()
                        .map(com.votricuong.mayhotel.documents.Customer::getId)
                        .collect(Collectors.toList());
                
                historyList = bookingOrderRepository.findAll().stream()
                        .filter(bo -> customerIds.contains(bo.getCustomerId()))
                        .sorted((a, b) -> b.getBookingDate().compareTo(a.getBookingDate()))
                        .collect(Collectors.toList());
            }
        }

        Map<Long, Room> phongMap = new HashMap<>();
        Map<Long, Invoice> hoaDonMap = new HashMap<>();
        Map<Long, Long> soDemMap = new HashMap<>();
        Map<Long, String> reviewStateMap = new HashMap<>();

        for (com.votricuong.mayhotel.documents.BookingOrder order : historyList) {
            List<com.votricuong.mayhotel.documents.BookingDetail> details =
                    bookingDetailRepository.findByBookingId(order.getId());
            if (details != null) {
                details.stream().filter(detail -> detail.getRoomId() != null).findFirst().ifPresent(detail ->
                        roomRepository.findById(detail.getRoomId())
                                .ifPresent(room -> phongMap.put(order.getId(), room)));
            }
            List<Invoice> invoices = invoiceRepository.findByBookingId(order.getId());
            if (invoices != null && !invoices.isEmpty()) {
                hoaDonMap.put(order.getId(), invoices.get(0));
            }
            soDemMap.put(order.getId(), calculateNights(order));
            reviewStateMap.put(order.getId(), reviewService.reviewState(order));
        }

        model.addAttribute("historyList", historyList);
        model.addAttribute("dsDatPhong", historyList);
        model.addAttribute("phongMap", phongMap);
        model.addAttribute("hoaDonMap", hoaDonMap);
        model.addAttribute("soDemMap", soDemMap);
        model.addAttribute("reviewStateMap", reviewStateMap);
        
        setPageTitle(model, "Lịch sử đặt phòng");
        setExtraCSS(model, "view/Booking/history :: extra_css");
        return "view/Booking/history";
    }

    // 10. HISTORY DETAIL
    @GetMapping("/history/detail")
    public String bookingHistoryDetail(@RequestParam("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<com.votricuong.mayhotel.documents.BookingOrder> orderOpt = bookingOrderRepository.findById(id);
        if (orderOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy đơn đặt phòng mã số này!");
            return "redirect:/booking/history";
        }

        com.votricuong.mayhotel.documents.BookingOrder order = orderOpt.get();
        
        List<com.votricuong.mayhotel.documents.BookingDetail> details = bookingDetailRepository.findByBookingId(id);
        if (!details.isEmpty() && details.get(0).getRoomId() != null) {
            roomRepository.findById(details.get(0).getRoomId()).ifPresent(room -> {
                model.addAttribute("phong", room);
            });
        }
        
        List<com.votricuong.mayhotel.documents.ServiceTicket> services = serviceTicketRepository.findByBookingId(id);

        setPageTitle(model, "Chi tiết đơn đặt phòng #" + order.getId());
        List<Invoice> invoices = invoiceRepository.findByBookingId(id);
        Invoice invoice = invoices != null && !invoices.isEmpty() ? invoices.get(0) : null;

        model.addAttribute("hoaDon", invoice != null ? invoice : order);
        model.addAttribute("order", order);
        model.addAttribute("soDem", calculateNights(order));
        customerRepository.findById(order.getCustomerId()).ifPresent(c -> model.addAttribute("customer", c));
        model.addAttribute("bookingDetails", details);
        model.addAttribute("serviceTickets", services);
        model.addAttribute("reviewState", reviewService.reviewState(order));
        model.addAttribute("myReview", reviewService.findReview(order.getId()).orElse(null));

        setExtraCSS(model, "view/Booking/historydetail :: extra_css");
        return "view/Booking/historydetail";
    }

    // 11. CANCEL BOOKING (UC03)
    @PostMapping("/cancel/{id}")
    public String cancelBooking(@PathVariable("id") Long id,
                                @RequestParam(value = "lyDoHuy", required = false) String reason,
                                HttpSession session,
                                RedirectAttributes ra) {
        Optional<com.votricuong.mayhotel.documents.BookingOrder> orderOpt = bookingOrderRepository.findById(id);
        if (orderOpt.isEmpty()) {
            ra.addFlashAttribute("error", "Không tìm thấy đơn đặt phòng mã số này!");
            return "redirect:/booking/history";
        }

        com.votricuong.mayhotel.documents.BookingOrder order = orderOpt.get();
        User user = (User) session.getAttribute("user");
        boolean ownsBooking = user != null && customerRepository.findById(order.getCustomerId())
                .map(customer -> user.getId().equals(customer.getUserId()))
                .orElse(false);
        if (!ownsBooking) {
            ra.addFlashAttribute("error", "Bạn không có quyền hủy phiếu đặt phòng này.");
            return "redirect:/booking/history";
        }
        try {
            bookingCancellationService.cancel(id,
                    com.votricuong.mayhotel.services.BookingCancellationService.CancellationType.CUSTOMER,
                    reason);
            ra.addFlashAttribute("success", "Hủy đặt phòng thành công. Chi tiết phí phạt và hoàn tiền đã được cập nhật trên hóa đơn.");
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/booking/history/detail?id=" + id;
    }

    private long calculateNights(com.votricuong.mayhotel.documents.BookingOrder order) {
        if (order.getExpectedIn() == null || order.getExpectedOut() == null) {
            return 0L;
        }
        long nights = (order.getExpectedOut().getTime() - order.getExpectedIn().getTime())
                / (1000L * 60 * 60 * 24);
        return Math.max(nights, 0L);
    }

    private HomeController.RoomTypeDTO toBookingRoomType(
            Room sample,
            com.votricuong.mayhotel.documents.RoomType type,
            List<Room> allRooms) {
        HomeController.RoomTypeDTO dto = new HomeController.RoomTypeDTO();
        dto.setMaLoai(sample.getRoomTypeId());
        dto.setName(type != null && type.getName() != null ? type.getName() : "Phòng Tiêu Chuẩn");
        dto.setSoNguoi(type != null && type.getMaxOccupancy() != null ? type.getMaxOccupancy() : 2);
        dto.setPrice(sample.getPrice());
        dto.setPhongs(allRooms.stream()
                .filter(room -> sample.getRoomTypeId().equals(room.getRoomTypeId()))
                .collect(Collectors.toList()));
        dto.setImageUrl(type != null && type.getImageUrl() != null ? type.getImageUrl() : "default.png");
        return dto;
    }
}
