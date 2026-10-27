package com.votricuong.mayhotel.controllers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.votricuong.mayhotel.documents.BookingDetail;
import com.votricuong.mayhotel.documents.Customer;
import com.votricuong.mayhotel.documents.Promotion;
import com.votricuong.mayhotel.documents.Review;
import com.votricuong.mayhotel.documents.Room;
import com.votricuong.mayhotel.documents.RoomType;
import com.votricuong.mayhotel.documents.User;
import com.votricuong.mayhotel.repositories.BookingDetailRepository;
import com.votricuong.mayhotel.repositories.CustomerRepository;
import com.votricuong.mayhotel.repositories.PromotionRepository;
import com.votricuong.mayhotel.repositories.ReviewRepository;
import com.votricuong.mayhotel.repositories.RoomRepository;
import com.votricuong.mayhotel.repositories.RoomTypeRepository;
import com.votricuong.mayhotel.repositories.ServiceRepository;

import jakarta.servlet.http.HttpSession;
import lombok.Data;

@Controller
public class HomeController extends BaseController {

    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final ServiceRepository serviceRepository;
    private final ReviewRepository reviewRepository;
    private final CustomerRepository customerRepository;
    private final PromotionRepository promotionRepository;

    public HomeController(RoomRepository roomRepository,
            RoomTypeRepository roomTypeRepository,
            BookingDetailRepository bookingDetailRepository,
            ServiceRepository serviceRepository,
            ReviewRepository reviewRepository,
            CustomerRepository customerRepository,
            PromotionRepository promotionRepository) {
        this.roomRepository = roomRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.bookingDetailRepository = bookingDetailRepository;
        this.serviceRepository = serviceRepository;
        this.reviewRepository = reviewRepository;
        this.customerRepository = customerRepository;
        this.promotionRepository = promotionRepository;
    }

    @Data
    public static class RoomTypeDTO implements Serializable {
        // Field cũ
        private Long maLoai;
        private String name;
        private Integer soNguoi;
        private Double price;
        private List<Room> phongs;
        private List<Room> phongsTrong;
        private String imageUrl;

        // Field mới
        private Long maPhong;
        private String soPhong;
        private String tenLoaiPhong;
        private String hinhAnhHienThi;
        private String moTa;
        private Integer sucChua;
        private Integer dienTich;
        private Double giaPhong;
        private String trangThai;
        private String viewDirection;
        private String locationZone;

        public RoomTypeDTO() {
            this.phongs = new ArrayList<>();
            this.phongsTrong = new ArrayList<>();
        }
    }

    // ================================================================
    // TRANG CHỦ
    // ================================================================
    @GetMapping("/")
    public String index(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user != null && user.getRoleId() != null) {
            long roleId = user.getRoleId();
            if (roleId == 1L)
                return "redirect:/admin/users";
            if (roleId == 2L)
                return "redirect:/manager/rooms";
            if (roleId == 3L)
                return "redirect:/receptionist/room-map";
            if (roleId == 7L)
                return "redirect:/admin/statistical";
        }

        // ── 1. PHÒNG NỔI BẬT ──
        List<Room> allRooms = roomRepository.findAll();
        Map<Long, RoomType> typeMap = roomTypeRepository.findAll().stream()
                .collect(Collectors.toMap(RoomType::getId, t -> t));

        List<BookingDetail> allDetails = bookingDetailRepository.findAll();
        Map<Long, Long> roomBookingCounts = allDetails.stream()
                .filter(d -> d.getRoomId() != null)
                .collect(Collectors.groupingBy(BookingDetail::getRoomId, Collectors.counting()));

        List<RoomTypeDTO> danhSach = allRooms.stream()
                .filter(r -> r.getRoomTypeId() != null)
                .map(r -> toDTO(r, typeMap.get(r.getRoomTypeId())))
                .collect(Collectors.toList());

        danhSach.sort((a, b) -> {
            long cA = roomBookingCounts.getOrDefault(a.getMaPhong(), 0L);
            long cB = roomBookingCounts.getOrDefault(b.getMaPhong(), 0L);
            if (cA != cB)
                return Long.compare(cB, cA);
            return Double.compare(
                    b.getGiaPhong() != null ? b.getGiaPhong() : 0,
                    a.getGiaPhong() != null ? a.getGiaPhong() : 0);
        });

        List<RoomTypeDTO> phongNoiBat = danhSach.stream().limit(6).collect(Collectors.toList());
        model.addAttribute("phongNoiBat", phongNoiBat);

        // ── 2. LOẠI PHÒNG ──
        model.addAttribute("loaiPhong", roomTypeRepository.findAll());

        // ── 3. DỊCH VỤ ──
        model.addAttribute("dichVuNoiBat", serviceRepository.findAll());

        // ── 4. REVIEWS ──
        Map<Long, String> customerNames = customerRepository.findAll().stream()
                .collect(Collectors.toMap(
                        Customer::getId,
                        c -> c.getFullName() != null ? c.getFullName() : "Khách hàng",
                        (a, b) -> a));

        List<Map<String, Object>> danhGiaNoiBat = reviewRepository.findAll().stream()
                .filter(rv -> rv.getStatus() == null || "Hiện".equalsIgnoreCase(rv.getStatus()))
                .sorted(Comparator.comparing(
                        Review::getReviewDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(6)
                .map(rv -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("tenKhachHang", customerNames.getOrDefault(rv.getCustomerId(), "Khách hàng"));
                    m.put("soSao", rv.getRating() != null ? rv.getRating() : 5);
                    m.put("binhLuan", rv.getContent() != null ? rv.getContent() : "");
                    m.put("ngayDanhGia", rv.getReviewDate());
                    return m;
                })
                .collect(Collectors.toList());
        model.addAttribute("danhGiaNoiBat", danhGiaNoiBat);

        // ── 5. FLASH SALE (promotion đang chạy) ──
        Date now = new Date();
        Promotion flashSale = promotionRepository.findAll().stream()
                .filter(p -> p.getStartDate() != null && p.getEndDate() != null)
                .filter(p -> !now.before(p.getStartDate()) && !now.after(p.getEndDate()))
                .max(Comparator.comparing(Promotion::getDiscountPercent,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);

        // Nếu không có promotion active → lấy promotion sắp diễn ra
        if (flashSale == null) {
            flashSale = promotionRepository.findAll().stream()
                    .filter(p -> p.getEndDate() != null && p.getEndDate().after(now))
                    .min(Comparator.comparing(Promotion::getStartDate,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .orElse(null);
        }

        // Nếu vẫn null → lấy promotion bất kỳ (demo)
        if (flashSale == null) {
            flashSale = promotionRepository.findAll().stream()
                    .max(Comparator.comparing(Promotion::getDiscountPercent,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .orElse(null);
        }

        model.addAttribute("flashSale", flashSale);

        // Đếm số phòng Trống (để hiển thị "chỉ còn X phòng")
        long soPhongTrong = allRooms.stream()
                .filter(r -> isVacant(r.getStatus()))
                .count();
        model.addAttribute("soPhongTrong", soPhongTrong);

        // Giây còn lại của flash sale
        if (flashSale != null && flashSale.getEndDate() != null) {
            long diff = flashSale.getEndDate().getTime() - now.getTime();
            model.addAttribute("flashSaleSeconds", Math.max(diff / 1000, 0));
        } else {
            model.addAttribute("flashSaleSeconds", 0);
        }

        return "view/home/index";
    }

    // ================================================================
    // INFO
    // ================================================================
    @GetMapping("/info")
    public String info(Model model) {
        model.addAttribute("pageTitle", "Thông tin khách sạn");
        return "view/home/info";
    }

    // ================================================================
    // DETAIL
    // ================================================================
    @GetMapping("/home/detail/{id}")
    public String detail(@PathVariable("id") Long id, Model model) {
        Room room = roomRepository.findById(id).orElse(null);
        if (room == null)
            return "redirect:/";

        if (room.getRoomTypeId() != null) {
            RoomType type = roomTypeRepository.findById(room.getRoomTypeId()).orElse(null);
            room.setRoomType(type);
            if ((room.getImageUrl() == null || room.getImageUrl().isBlank()) && type != null) {
                room.setImageUrl(type.getImageUrl());
            }
        }

        model.addAttribute("phong", room);
        model.addAttribute("conPhong", isVacant(room.getStatus()));
        model.addAttribute("pageTitle", "Chi tiết phòng " + room.getName());
        return "view/home/detail";
    }

    // ================================================================
    // REVIEW
    // ================================================================
    @GetMapping("/review")
    public String review(Model model) {
        model.addAttribute("pageTitle", "Đánh giá từ khách hàng");
        return "view/home/review";
    }

    // ================================================================
    // HELPERS
    // ================================================================
    private RoomTypeDTO toDTO(Room r, RoomType t) {
        RoomTypeDTO dto = new RoomTypeDTO();
        dto.setMaPhong(r.getId());
        dto.setSoPhong(r.getName());

        String typeName = (t != null && t.getName() != null) ? t.getName() : "Phòng";
        dto.setTenLoaiPhong(typeName);

        String img = r.getImageUrl();
        if (img == null || img.isBlank()) {
            img = (t != null && t.getImageUrl() != null) ? t.getImageUrl() : "default.jpg";
        }
        dto.setHinhAnhHienThi(img);

        dto.setSucChua(t != null && t.getMaxOccupancy() != null ? t.getMaxOccupancy() : 2);
        dto.setDienTich(null);
        dto.setGiaPhong(r.getPrice() != null ? r.getPrice() : 0.0);
        dto.setTrangThai(isVacant(r.getStatus()) ? "Còn phòng" : "Hết phòng");
        dto.setViewDirection(r.getViewDirection());
        dto.setLocationZone(r.getLocationZone());
        dto.setMoTa(buildMoTa(r, t));

        dto.setMaLoai(r.getRoomTypeId());
        dto.setName(typeName);
        dto.setSoNguoi(dto.getSucChua());
        dto.setPrice(dto.getGiaPhong());
        dto.setImageUrl(img);
        dto.setPhongs(new ArrayList<>(List.of(r)));
        if (isVacant(r.getStatus())) {
            dto.setPhongsTrong(new ArrayList<>(List.of(r)));
        }
        return dto;
    }

    private boolean isVacant(String status) {
        if (status == null)
            return false;
        String s = status.trim();
        return "Vacant".equalsIgnoreCase(s)
                || "Còn phòng".equalsIgnoreCase(s)
                || "Trống".equalsIgnoreCase(s);
    }

    private String buildMoTa(Room r, RoomType t) {
        StringBuilder sb = new StringBuilder();
        if (t != null && t.getName() != null)
            sb.append(t.getName());
        if (r.getViewDirection() != null && !r.getViewDirection().isBlank()) {
            if (sb.length() > 0)
                sb.append(" — ");
            sb.append(r.getViewDirection());
        }
        if (sb.length() == 0)
            return "Phòng tiện nghi, phù hợp lưu trú ngắn và dài ngày.";
        return sb.append(". Phòng tiện nghi, đầy đủ tiện ích.").toString();
    }
}
