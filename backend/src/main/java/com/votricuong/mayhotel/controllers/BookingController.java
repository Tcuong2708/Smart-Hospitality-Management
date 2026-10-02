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

    public BookingController(RoomRepository roomRepository, 
                             ServiceRepository serviceRepository, 
                             com.votricuong.mayhotel.repositories.BookingOrderRepository bookingOrderRepository,
                             com.votricuong.mayhotel.repositories.BookingDetailRepository bookingDetailRepository,
                             com.votricuong.mayhotel.repositories.ServiceTicketRepository serviceTicketRepository,
                             com.votricuong.mayhotel.repositories.CustomerRepository customerRepository,
                             com.votricuong.mayhotel.services.SequenceGeneratorService sequenceGeneratorService,
                             InvoiceRepository invoiceRepository,
                             com.votricuong.mayhotel.repositories.RoomTypeRepository roomTypeRepository) {
        this.roomRepository = roomRepository;
        this.serviceRepository = serviceRepository;
        this.bookingOrderRepository = bookingOrderRepository;
        this.bookingDetailRepository = bookingDetailRepository;
        this.serviceTicketRepository = serviceTicketRepository;
        this.customerRepository = customerRepository;
        this.sequenceGeneratorService = sequenceGeneratorService;
        this.invoiceRepository = invoiceRepository;
        this.roomTypeRepository = roomTypeRepository;
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
            .map(r -> HomeController.RoomTypeDTO.builder()
                    .maLoai(r.getRoomTypeId())
                    .name(type != null && type.getName() != null ? type.getName() : "Phòng Tiêu Chuẩn")
                    .soNguoi(type != null && type.getMaxOccupancy() != null ? type.getMaxOccupancy() : 2)
                    .price(r.getPrice())
                    .phongs(allRooms.stream().filter(rr -> r.getRoomTypeId().equals(rr.getRoomTypeId())).collect(Collectors.toList()))
                    .imageUrl(type != null && type.getImageUrl() != null ? type.getImageUrl() : "default.png")
                    .build())
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
        return render(model, "view/Booking/booking");
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
            .map(r -> HomeController.RoomTypeDTO.builder()
                    .maLoai(r.getRoomTypeId())
                    .name(type != null && type.getName() != null ? type.getName() : "Phòng Tiêu Chuẩn")
                    .soNguoi(type != null && type.getMaxOccupancy() != null ? type.getMaxOccupancy() : 2)
                    .price(r.getPrice())
                    .phongs(allRooms.stream().filter(rr -> r.getRoomTypeId().equals(rr.getRoomTypeId())).collect(Collectors.toList()))
                    .imageUrl(type != null && type.getImageUrl() != null ? type.getImageUrl() : "default.png")
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
            .map(r -> HomeController.RoomTypeDTO.builder()
                    .maLoai(r.getRoomTypeId())
                    .name(type != null && type.getName() != null ? type.getName() : "Phòng Tiêu Chuẩn")
                    .soNguoi(type != null && type.getMaxOccupancy() != null ? type.getMaxOccupancy() : 2)
                    .price(r.getPrice())
                    .phongs(allRooms.stream().filter(rr -> maLoai.equals(rr.getRoomTypeId())).collect(Collectors.toList()))
                    .imageUrl(type != null && type.getImageUrl() != null ? type.getImageUrl() : "default.png")
                    .build())
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
        // dummyUser.setFullName("");
        // dummyUser.setPhone("");
        model.addAttribute("currentUser", dummyUser);

        setExtraCSS(model, "view/Booking/checkout :: extra_css");
        return render(model, "view/Booking/checkout");
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
                    .filter(p -> p.getRoomTypeId() != null && p.getRoomTypeId().equals(maLoai) && 
                           ("Vacant".equalsIgnoreCase(p.getStatus()) || 
                            "Còn phòng".equalsIgnoreCase(p.getStatus()) || 
                            "Trống".equalsIgnoreCase(p.getStatus()) || 
                            "Phòng trống".equalsIgnoreCase(p.getStatus())))
                    .collect(Collectors.toList());

            if (danhSachPhongTrong.size() < soLuongDat) {
                redirectAttributes.addFlashAttribute("error", "Ngày bạn chọn đã hết phòng! Vui lòng đổi ngày khác.");
                return "redirect:/booking/checkout";
            }
            
            // Xử lý Customer
            com.votricuong.mayhotel.documents.Customer customer = null;
            if (phone != null && !phone.isEmpty()) {
                List<com.votricuong.mayhotel.documents.Customer> existingCustomers = customerRepository.findByPhone(phone);
                if (!existingCustomers.isEmpty()) {
                    customer = existingCustomers.get(0);
                }
            }
            
            User sessionUser = (User) session.getAttribute("user");

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

            // Tạo BookingOrder
            com.votricuong.mayhotel.documents.BookingOrder bookingOrder = new com.votricuong.mayhotel.documents.BookingOrder();
            bookingOrder.setId(sequenceGeneratorService.generateSequence("booking_orders_sequence")); 
            bookingOrder.setCustomerId(customer.getId());
            bookingOrder.setBookingDate(new Date());
            bookingOrder.setExpectedIn(Date.from(currentBooking.getNgayNhan().atStartOfDay(ZoneId.systemDefault()).toInstant()));
            bookingOrder.setExpectedOut(Date.from(currentBooking.getNgayTra().atStartOfDay(ZoneId.systemDefault()).toInstant()));
            bookingOrder.setStatus("Pending");
            
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
                // st.setPrice(s.getPrice());
                // st.setOrderDate(new Date());
                // st.setStatus("Delivered");
                serviceTicketRepository.save(st);
            }

            // Xóa session
            session.removeAttribute("CurrentBooking");
            session.removeAttribute("selectedServiceIds");

            // TẠO INVOICE ĐỂ LỄ TÂN CÓ THỂ THẤY Ở MÀN HÌNH CHECK-IN
            Invoice invoice = new Invoice();
            invoice.setId(sequenceGeneratorService.generateSequence("invoices_sequence"));
            invoice.setBookingId(generatedMaHD);
            invoice.setCreatedAt(new Date());
            invoice.setTotalAmount(tongGiaCacPhong + totalDichVu);
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
            invoiceRepository.save(invoice);

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

        model.addAttribute("historyList", historyList);
        
        setPageTitle(model, "Lịch sử đặt phòng");
        setExtraCSS(model, "view/Booking/history :: extra_css");
        return render(model, "view/Booking/history");
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
        if (!details.isEmpty()) {
            roomRepository.findById(details.get(0).getRoomId()).ifPresent(room -> {
                model.addAttribute("phong", room);
            });
        }
        
        List<com.votricuong.mayhotel.documents.ServiceTicket> services = serviceTicketRepository.findByBookingId(id);

        setPageTitle(model, "Chi tiết đơn đặt phòng #" + order.getId());
        model.addAttribute("hoaDon", order); // Keep the attribute name "hoaDon" for frontend compatibility if needed
        model.addAttribute("order", order); // Required for historydetail.html
        customerRepository.findById(order.getCustomerId()).ifPresent(c -> model.addAttribute("customer", c));
        model.addAttribute("bookingDetails", details);
        model.addAttribute("serviceTickets", services);

        setExtraCSS(model, "view/Booking/historydetail :: extra_css");
        return render(model, "view/Booking/historydetail");
    }

    // 11. CANCEL BOOKING (UC03)
    @PostMapping("/cancel/{id}")
    public String cancelBooking(@PathVariable("id") Long id, RedirectAttributes ra) {
        Optional<com.votricuong.mayhotel.documents.BookingOrder> orderOpt = bookingOrderRepository.findById(id);
        if (orderOpt.isEmpty()) {
            ra.addFlashAttribute("error", "Không tìm thấy đơn đặt phòng mã số này!");
            return "redirect:/booking/history";
        }

        com.votricuong.mayhotel.documents.BookingOrder order = orderOpt.get();
        
        // Theo UC03: Hủy được nếu phiếu đang chờ nhận phòng
        if ("Đã nhận phòng".equalsIgnoreCase(order.getStatus()) || "Đã hủy".equalsIgnoreCase(order.getStatus()) || "Hủy do khách không đến".equalsIgnoreCase(order.getStatus())) {
            ra.addFlashAttribute("error", "Không thể hủy đơn đặt phòng đang ở trạng thái này.");
            return "redirect:/booking/history/detail?id=" + id;
        }

        // Đổi trạng thái
        order.setStatus("Đã hủy");
        bookingOrderRepository.save(order);

        // Nhả phòng trống về kho
        List<com.votricuong.mayhotel.documents.BookingDetail> details = bookingDetailRepository.findByBookingId(id);
        for (com.votricuong.mayhotel.documents.BookingDetail bd : details) {
            roomRepository.findById(bd.getRoomId()).ifPresent(room -> {
                room.setStatus("Trống");
                room.setNote("");
                roomRepository.save(room);
            });
        }

        ra.addFlashAttribute("success", "Hủy đặt phòng thành công!");
        return "redirect:/booking/history/detail?id=" + id;
    }
}
