package com.votricuong.mayhotel.controllers;

import com.votricuong.mayhotel.documents.BookingOrder;
import com.votricuong.mayhotel.documents.Customer;
import com.votricuong.mayhotel.documents.Room;
import com.votricuong.mayhotel.repositories.BookingDetailRepository;
import com.votricuong.mayhotel.repositories.BookingOrderRepository;
import com.votricuong.mayhotel.repositories.CustomerRepository;
import com.votricuong.mayhotel.repositories.RoomRepository;
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

@Controller
@RequestMapping("/receptionist/booking")
public class ReceptionistBookingController extends BaseController {

    private final RoomRepository roomRepository;
    private final CustomerRepository customerRepository;
    private final BookingOrderRepository bookingOrderRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final SequenceGeneratorService sequenceGeneratorService;

    public ReceptionistBookingController(RoomRepository roomRepository,
                                         CustomerRepository customerRepository,
                                         BookingOrderRepository bookingOrderRepository,
                                         BookingDetailRepository bookingDetailRepository,
                                         SequenceGeneratorService sequenceGeneratorService) {
        this.roomRepository = roomRepository;
        this.customerRepository = customerRepository;
        this.bookingOrderRepository = bookingOrderRepository;
        this.bookingDetailRepository = bookingDetailRepository;
        this.sequenceGeneratorService = sequenceGeneratorService;
    }

    @GetMapping
    public String index(Model model) {
        List<Room> vacantRooms = roomRepository.findAll().stream()
                .filter(r -> "Vacant".equalsIgnoreCase(r.getStatus()) || "Phòng trống".equalsIgnoreCase(r.getStatus()))
                .collect(Collectors.toList());

        setPageTitle(model, "Lễ tân - Đặt phòng trực tiếp");
        model.addAttribute("vacantRooms", vacantRooms);
        return render(model, "view/Receptionist/booking/index");
    }

    @PostMapping("/checkout")
    public String checkout(
            @RequestParam("roomId") Long roomId,
            @RequestParam("guestName") String guestName,
            @RequestParam("phone") String phone,
            @RequestParam("ngayNhan") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate ngayNhan,
            @RequestParam("ngayTra") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate ngayTra,
            RedirectAttributes ra) {

        try {
            Optional<Room> roomOpt = roomRepository.findById(roomId);
            if (roomOpt.isEmpty()) {
                ra.addFlashAttribute("error", "Không tìm thấy phòng này!");
                return "redirect:/receptionist/booking";
            }

            Room room = roomOpt.get();
            if (!"Vacant".equalsIgnoreCase(room.getStatus()) && !"Phòng trống".equalsIgnoreCase(room.getStatus())) {
                ra.addFlashAttribute("error", "Phòng này không còn trống!");
                return "redirect:/receptionist/booking";
            }

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
            
            // UC21: Tại quầy, có thể chuyển thẳng sang "Đã nhận phòng" nếu check-in luôn, ở đây giả định "Đã xác nhận cọc" hoặc "Đã nhận phòng"
            order.setStatus("Đã nhận phòng");
            bookingOrderRepository.save(order);

            // Booking Detail
            com.votricuong.mayhotel.documents.BookingDetail bd = new com.votricuong.mayhotel.documents.BookingDetail();
            bd.setId(sequenceGeneratorService.generateSequence("booking_details_sequence"));
            bd.setBookingId(order.getId());
            bd.setRoomId(room.getId());
            bookingDetailRepository.save(bd);

            // Update Room
            room.setStatus("Đang sử dụng");
            roomRepository.save(room);

            ra.addFlashAttribute("success", "Tạo đơn đặt phòng thành công cho khách " + guestName);

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }

        return "redirect:/receptionist/booking";
    }
}
