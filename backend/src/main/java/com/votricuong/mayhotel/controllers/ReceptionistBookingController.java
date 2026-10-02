package com.votricuong.mayhotel.controllers;

import com.votricuong.mayhotel.documents.BookingOrder;
import com.votricuong.mayhotel.documents.Customer;
import com.votricuong.mayhotel.documents.Room;
import com.votricuong.mayhotel.repositories.BookingDetailRepository;
import com.votricuong.mayhotel.repositories.BookingOrderRepository;
import com.votricuong.mayhotel.repositories.CustomerRepository;
import com.votricuong.mayhotel.repositories.RoomRepository;
import com.votricuong.mayhotel.repositories.RoomTypeRepository;
import com.votricuong.mayhotel.services.SequenceGeneratorService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.votricuong.mayhotel.documents.BookingDetail;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/receptionist/booking")
public class ReceptionistBookingController extends BaseController {

    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final CustomerRepository customerRepository;
    private final BookingOrderRepository bookingOrderRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final SequenceGeneratorService sequenceGeneratorService;

    public ReceptionistBookingController(RoomRepository roomRepository,
                                         RoomTypeRepository roomTypeRepository,
                                         CustomerRepository customerRepository,
                                         BookingOrderRepository bookingOrderRepository,
                                         BookingDetailRepository bookingDetailRepository,
                                         SequenceGeneratorService sequenceGeneratorService) {
        this.roomRepository = roomRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.customerRepository = customerRepository;
        this.bookingOrderRepository = bookingOrderRepository;
        this.bookingDetailRepository = bookingDetailRepository;
        this.sequenceGeneratorService = sequenceGeneratorService;
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
        return render(model, "view/Receptionist/booking/index");
    }

    @PostMapping("/checkout")
    public String checkout(
            @RequestParam(value = "roomId", required = false) Long roomId,
            @RequestParam("guestName") String guestName,
            @RequestParam("phone") String phone,
            @RequestParam("ngayNhan") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate ngayNhan,
            @RequestParam("ngayTra") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate ngayTra,
            RedirectAttributes ra) {

        try {
            Room room = null;
            if (roomId != null) {
                Optional<Room> roomOpt = roomRepository.findById(roomId);
                if (roomOpt.isPresent()) {
                    room = roomOpt.get();
                }
            }
            // Đã bỏ chặn bắt buộc phòng phải trống hiện tại để cho phép đặt trước (Future Booking)
            
            // Customer
            Customer customer = customerRepository.findByPhone(phone).orElseGet(() -> {
                Customer newCus = new Customer();
                newCus.setId(sequenceGeneratorService.generateSequence("customers_sequence"));
                newCus.setFullName(guestName);
                newCus.setPhone(phone);
                return customerRepository.save(newCus);
            });

            // Booking Order
            BookingOrder order = new BookingOrder();
            order.setId(sequenceGeneratorService.generateSequence("booking_orders_sequence"));
            order.setCustomerId(customer.getId());
            order.setBookingDate(new Date());
            order.setExpectedIn(Date.from(ngayNhan.atStartOfDay(ZoneId.systemDefault()).toInstant()));
            order.setExpectedOut(Date.from(ngayTra.atStartOfDay(ZoneId.systemDefault()).toInstant()));
            
            // Xử lý logic Đặt trước (Future Booking) vs Đặt lấy ngay (Walk-in)
            LocalDate today = LocalDate.now();
            // Nếu ngày nhận là tương lai HOẶC chưa gán phòng cụ thể -> Trạng thái là Pending
            if (ngayNhan.isAfter(today) || room == null) {
                order.setStatus("Pending");
                bookingOrderRepository.save(order);
                // Không đổi trạng thái thực tế của phòng vì phòng chưa gán hoặc đặt cho tương lai
            } else {
                // Đặt lấy ngay trong ngày và ĐÃ GÁN phòng -> Đã nhận phòng
                order.setStatus("Đã nhận phòng");
                bookingOrderRepository.save(order);
                
                room.setStatus("Đang sử dụng");
                roomRepository.save(room);
            }

            // Booking Detail
            com.votricuong.mayhotel.documents.BookingDetail bd = new com.votricuong.mayhotel.documents.BookingDetail();
            bd.setId(sequenceGeneratorService.generateSequence("booking_details_sequence"));
            bd.setBookingId(order.getId());
            if (room != null) {
                bd.setRoomId(room.getId());
            }
            bookingDetailRepository.save(bd);

            ra.addFlashAttribute("success", "Tạo đơn đặt phòng thành công cho khách " + guestName);

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }

        return "redirect:/receptionist/booking";
    }
}
