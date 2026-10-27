package com.votricuong.mayhotel.controllers;

import com.votricuong.mayhotel.documents.BookingOrder;
import com.votricuong.mayhotel.documents.Customer;
import com.votricuong.mayhotel.documents.Room;
import com.votricuong.mayhotel.repositories.BookingDetailRepository;
import com.votricuong.mayhotel.repositories.BookingOrderRepository;
import com.votricuong.mayhotel.repositories.CustomerRepository;
import com.votricuong.mayhotel.repositories.RoomRepository;
import com.votricuong.mayhotel.repositories.RoomTypeRepository;
import com.votricuong.mayhotel.repositories.InvoiceRepository;
import com.votricuong.mayhotel.documents.Invoice;
import com.votricuong.mayhotel.services.SequenceGeneratorService;
import com.votricuong.mayhotel.services.BookingAvailabilityService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.votricuong.mayhotel.documents.BookingDetail;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Calendar;
import java.time.temporal.ChronoUnit;

@Controller
@RequestMapping({"/receptionist/booking"})
public class ReceptionistBookingController extends BaseController {

    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final CustomerRepository customerRepository;
    private final BookingOrderRepository bookingOrderRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final InvoiceRepository invoiceRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final BookingAvailabilityService bookingAvailabilityService;
    private final com.votricuong.mayhotel.services.NoShowPredictionService noShowPredictionService;

    public ReceptionistBookingController(RoomRepository roomRepository,
                                         RoomTypeRepository roomTypeRepository,
                                         CustomerRepository customerRepository,
                                         BookingOrderRepository bookingOrderRepository,
                                         BookingDetailRepository bookingDetailRepository,
                                         InvoiceRepository invoiceRepository,
                                         SequenceGeneratorService sequenceGeneratorService,
                                         BookingAvailabilityService bookingAvailabilityService,
                                         com.votricuong.mayhotel.services.NoShowPredictionService noShowPredictionService) {
        this.roomRepository = roomRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.customerRepository = customerRepository;
        this.bookingOrderRepository = bookingOrderRepository;
        this.bookingDetailRepository = bookingDetailRepository;
        this.invoiceRepository = invoiceRepository;
        this.sequenceGeneratorService = sequenceGeneratorService;
        this.bookingAvailabilityService = bookingAvailabilityService;
        this.noShowPredictionService = noShowPredictionService;
    }

    @GetMapping
    public String index(Model model) {
        // Lấy tất cả phòng thay vì chỉ phòng trống để Lễ tân có thể đặt trước cho tương lai
        List<Room> vacantRooms = roomRepository.findAll();

        List<BookingOrder> activeBookings = bookingOrderRepository.findAll();
        List<Map<String, Object>> displayBookings = new ArrayList<>();
        
        for (BookingOrder order : activeBookings) {
            Map<String, Object> map = new HashMap<>();
            map.put("order", order);
            
            String customerName = "Khách vãng lai";
            if (order.getCustomerId() != null) {
                Customer cus = customerRepository.findById(order.getCustomerId()).orElse(null);
                if (cus != null) {
                    customerName = cus.getFullName() != null ? cus.getFullName() : cus.getPhone();
                }
            }
            map.put("customerName", customerName);
            
            String roomName = "Chưa gán";
            List<BookingDetail> details = bookingDetailRepository.findByBookingId(order.getId());
            if (details != null && !details.isEmpty()) {
                Long rId = details.get(0).getRoomId();
                if (rId != null) {
                    Room r = roomRepository.findById(rId).orElse(null);
                    if (r != null) {
                        roomName = r.getName() != null ? r.getName() : ("Phòng " + r.getId());
                    }
                }
            }
            map.put("roomName", roomName);
            
            // AI Risk status
            String riskBadge = "bg-warning text-dark";
            if ("Thấp".equals(order.getNoShowRiskLevel())) riskBadge = "bg-success";
            else if ("Cao".equals(order.getNoShowRiskLevel())) riskBadge = "bg-danger";
            
            map.put("riskBadge", riskBadge);
            
            displayBookings.add(map);
        }

        // Sort descending
        displayBookings.sort((a, b) -> {
            BookingOrder o1 = (BookingOrder) a.get("order");
            BookingOrder o2 = (BookingOrder) b.get("order");
            if (o1.getBookingDate() == null) return 1;
            if (o2.getBookingDate() == null) return -1;
            return o2.getBookingDate().compareTo(o1.getBookingDate());
        });

        setPageTitle(model, "Lễ tân - Đặt phòng trực tiếp");
        model.addAttribute("vacantRooms", vacantRooms);
        model.addAttribute("bookings", displayBookings);
        model.addAttribute("roomTypes", roomTypeRepository.findAll());
        setExtraCSS(model, "view/Receptionist/booking/index :: extra_css");
        setExtraJS(model, "view/Receptionist/booking/index :: extra_js");
        return render(model, "view/Receptionist/booking/index");
    }

    @PostMapping("/checkout")
    public synchronized String checkout(
            @RequestParam(value = "roomId", required = false) Long roomId,
            @RequestParam("guestName") String guestName,
            @RequestParam("phone") String phone,
            @RequestParam(value = "guestCount", defaultValue = "1") Integer guestCount,
            @RequestParam("ngayNhan") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate ngayNhan,
            @RequestParam("ngayTra") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate ngayTra,
            RedirectAttributes ra) {

        try {
            guestName = com.votricuong.mayhotel.utils.InputValidator.requiredText(guestName, "Họ và tên", 100);
            phone = com.votricuong.mayhotel.utils.InputValidator.phone(phone);
            com.votricuong.mayhotel.utils.InputValidator.stay(ngayNhan, ngayTra);
            com.votricuong.mayhotel.utils.InputValidator.range(guestCount, "Số khách lưu trú", 1, 100);

            Date expectedIn = Date.from(ngayNhan.atStartOfDay(ZoneOffset.UTC).toInstant());
            Date expectedOut = Date.from(ngayTra.atStartOfDay(ZoneOffset.UTC).toInstant());
            bookingAvailabilityService.validateStay(expectedIn, expectedOut);

            Room room = null;
            if (roomId != null) {
                Optional<Room> roomOpt = roomRepository.findById(roomId);
                if (roomOpt.isPresent()) {
                    room = roomOpt.get();
                }
            }
            if (room == null) {
                throw new IllegalArgumentException("Vui lòng chọn một phòng hợp lệ");
            }
            if (!bookingAvailabilityService.isRoomAvailable(room.getId(), expectedIn, expectedOut)) {
                throw new IllegalStateException("Phòng đã được đặt trong khoảng thời gian này, vui lòng chọn phòng khác");
            }
            int maxOccupancy = roomTypeRepository.findById(room.getRoomTypeId())
                    .map(type -> type.getMaxOccupancy() == null ? 2 : type.getMaxOccupancy())
                    .orElse(2);
            if (guestCount > maxOccupancy) {
                throw new IllegalArgumentException("Phòng đã chọn chỉ chứa tối đa " + maxOccupancy + " khách.");
            }
            
            // Customer
            Customer customer = null;
            List<Customer> existingCustomers = customerRepository.findByPhone(phone);
            if (existingCustomers != null && !existingCustomers.isEmpty()) {
                customer = existingCustomers.get(0);
            }
            if (customer == null) {
                Customer newCus = new Customer();
                newCus.setId(sequenceGeneratorService.generateSequence("customers_sequence"));
                newCus.setFullName(guestName);
                newCus.setPhone(phone);
                customer = customerRepository.save(newCus);
            }

            // Booking Order
            BookingOrder order = new BookingOrder();
            order.setId(sequenceGeneratorService.generateSequence("booking_orders_sequence"));
            order.setCustomerId(customer.getId());
            order.setBookingDate(new Date());
            order.setExpectedIn(expectedIn);
            order.setExpectedOut(expectedOut);
            
            // Luôn đặt trạng thái là Pending để chuyển sang quy trình Làm thủ tục nhận phòng (Check-in - UC05)
            // Lễ tân sẽ cập nhật trạng thái phòng thực tế tại giao diện Check-in
            order.setStatus("Pending");
            order.setBookingSource("RECEPTION");
            order.setRiskAnalysisStatus("PENDING");
            order.setAutoCancelTime(calculateAutoCancelTime(order));
            bookingOrderRepository.save(order);

            // Booking Detail
            com.votricuong.mayhotel.documents.BookingDetail bd = new com.votricuong.mayhotel.documents.BookingDetail();
            bd.setId(sequenceGeneratorService.generateSequence("booking_details_sequence"));
            bd.setBookingId(order.getId());
            if (room != null) {
                bd.setRoomId(room.getId());
                bd.setUnitPrice(room.getPrice());
            }
            bookingDetailRepository.save(bd);

            // Bổ sung: Tạo Invoice để đơn xuất hiện trong mục Nhận Phòng (Check-in)
            Invoice invoice = new Invoice();
            invoice.setId(sequenceGeneratorService.generateSequence("invoices_sequence"));
            invoice.setBookingId(order.getId());
            invoice.setGuestName(guestName);
            invoice.setPhone(phone);
            invoice.setGuestCount(guestCount);
            invoice.setCheckInDate(order.getExpectedIn());
            invoice.setCheckOutDate(order.getExpectedOut());
            invoice.setCreatedAt(new Date());
            if (room != null) {
                invoice.setRoomId(room.getId());
                long nights = ChronoUnit.DAYS.between(ngayNhan, ngayTra);
                double unitPrice = room.getPrice() != null ? room.getPrice() : 0.0;
                invoice.setGrossAmount(unitPrice * nights);
                invoice.setTotalAmount(unitPrice * nights);
            }
            // Luôn gán Reserved để hiển thị ở danh sách chờ Check-in
            invoice.setInvoiceStatus("Reserved");
            invoice.setIsPaid(false);
            invoice.setSurcharge(0.0);
            invoiceRepository.save(invoice);
            noShowPredictionService.analyzeAndSave(order.getId());

            ra.addFlashAttribute("success", "Tạo đơn đặt phòng thành công cho khách " + guestName);

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }

        return "redirect:/receptionist/booking";
    }

    private Date calculateAutoCancelTime(BookingOrder order) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(order.getExpectedIn());
        calendar.set(Calendar.HOUR_OF_DAY, "Cao".equals(order.getNoShowRiskLevel()) ? 14 : 18);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }
}
