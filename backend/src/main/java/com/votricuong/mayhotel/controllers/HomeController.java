package com.votricuong.mayhotel.controllers;

import java.io.Serializable;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.votricuong.mayhotel.documents.BookingDetail;
import com.votricuong.mayhotel.documents.Customer;
import com.votricuong.mayhotel.documents.Promotion;
import com.votricuong.mayhotel.documents.Review;
import com.votricuong.mayhotel.documents.Room;
import com.votricuong.mayhotel.documents.RoomType;
import com.votricuong.mayhotel.documents.User;
import com.votricuong.mayhotel.repositories.BookingDetailRepository;
import com.votricuong.mayhotel.repositories.CustomerRepository;
import com.votricuong.mayhotel.repositories.ReviewRepository;
import com.votricuong.mayhotel.repositories.RoomRepository;
import com.votricuong.mayhotel.repositories.RoomTypeRepository;
import com.votricuong.mayhotel.repositories.ServiceRepository;
import com.votricuong.mayhotel.services.ReviewService;
import com.votricuong.mayhotel.services.PromotionService;

import jakarta.servlet.http.HttpSession;
import lombok.Data;

@Controller
public class HomeController extends BaseController {

    public record DirectionOption(String value, String label) {}

    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final ServiceRepository serviceRepository;
    private final ReviewRepository reviewRepository;
    private final CustomerRepository customerRepository;
    private final PromotionService promotionService;
    private final ReviewService reviewService;

    public HomeController(RoomRepository roomRepository,
            RoomTypeRepository roomTypeRepository,
            BookingDetailRepository bookingDetailRepository,
            ServiceRepository serviceRepository,
            ReviewRepository reviewRepository,
            CustomerRepository customerRepository,
            PromotionService promotionService,
            ReviewService reviewService) {
        this.roomRepository = roomRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.bookingDetailRepository = bookingDetailRepository;
        this.serviceRepository = serviceRepository;
        this.reviewRepository = reviewRepository;
        this.customerRepository = customerRepository;
        this.promotionService = promotionService;
        this.reviewService = reviewService;
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
        private Double diemDanhGia;
        private Long soDanhGia;
        private Long soLuotDat;

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
            if (roleId == 8L)
                return "redirect:/admin/roles";
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
        List<RoomType> roomTypes = roomTypeRepository.findAll();
        Map<Long, RoomType> typeMap = roomTypes.stream()
                .collect(Collectors.toMap(RoomType::getId, t -> t));
        Map<String, String> directionOptions = new LinkedHashMap<>();
        allRooms.stream().map(Room::getViewDirection).map(this::normalizeDirection)
                .filter(value -> !value.isBlank())
                .forEach(value -> directionOptions.putIfAbsent(value, directionLabel(value)));
        model.addAttribute("roomTypes", roomTypes);
        model.addAttribute("directionOptions", directionOptions.entrySet().stream()
                .map(entry -> new DirectionOption(entry.getKey(), entry.getValue())).toList());

        List<BookingDetail> allDetails = bookingDetailRepository.findAll();
        List<Review> publicReviews = reviewRepository.findAll().stream()
                .filter(review -> review.getRating() != null
                        && (review.getStatus() == null || "Hiện".equalsIgnoreCase(review.getStatus())))
                .filter(review -> review.getRoomTypeIds() != null)
                .toList();

        Map<Long, Double> roomTypeRatings = publicReviews.stream()
                .flatMap(review -> review.getRoomTypeIds().stream()
                        .map(typeId -> Map.entry(typeId, review.getRating().doubleValue())))
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.averagingDouble(Map.Entry::getValue)));

        Map<Long, Long> roomTypeReviewCounts = publicReviews.stream()
                .flatMap(review -> review.getRoomTypeIds().stream())
                .collect(Collectors.groupingBy(typeId -> typeId, Collectors.counting()));

        Map<Long, Room> roomById = allRooms.stream()
                .filter(room -> room.getId() != null)
                .collect(Collectors.toMap(Room::getId, room -> room, (first, ignored) -> first));
        Map<Long, Long> roomTypeBookingCounts = allDetails.stream()
                .map(BookingDetail::getRoomId)
                .map(roomById::get)
                .filter(room -> room != null && room.getRoomTypeId() != null)
                .collect(Collectors.groupingBy(Room::getRoomTypeId, Collectors.counting()));

        List<RoomTypeDTO> danhSach = allRooms.stream()
                .filter(r -> r.getRoomTypeId() != null)
                .map(r -> toDTO(r, typeMap.get(r.getRoomTypeId())))
                .collect(Collectors.toList());

        // Giao diện khách hàng chỉ giới thiệu loại phòng; không lặp hoặc công khai số phòng vật lý.
        List<RoomTypeDTO> uniqueTypes = new ArrayList<>(danhSach.stream()
                .filter(dto -> dto.getMaLoai() != null)
                .collect(Collectors.toMap(RoomTypeDTO::getMaLoai, dto -> dto, (first, ignored) -> first,
                        LinkedHashMap::new))
                .values());
        Map<Long, List<Room>> roomsByType = allRooms.stream()
                .filter(room -> room.getRoomTypeId() != null)
                .collect(Collectors.groupingBy(Room::getRoomTypeId));
        uniqueTypes.forEach(dto -> {
            dto.setHinhAnhHienThi(selectTypeImage(
                    typeMap.get(dto.getMaLoai()),
                    roomsByType.getOrDefault(dto.getMaLoai(), List.of())));
            dto.setImageUrl(dto.getHinhAnhHienThi());
            dto.setDiemDanhGia(roomTypeRatings.getOrDefault(dto.getMaLoai(), 0D) * 2D);
            dto.setSoDanhGia(roomTypeReviewCounts.getOrDefault(dto.getMaLoai(), 0L));
            dto.setSoLuotDat(roomTypeBookingCounts.getOrDefault(dto.getMaLoai(), 0L));
        });

        List<RoomTypeDTO> phongNoiBat = uniqueTypes.stream()
                .filter(dto -> dto.getSoDanhGia() != null && dto.getSoDanhGia() > 0)
                .sorted(Comparator.comparing(RoomTypeDTO::getDiemDanhGia).reversed()
                        .thenComparing(RoomTypeDTO::getSoDanhGia, Comparator.reverseOrder())
                        .thenComparing(RoomTypeDTO::getSoLuotDat, Comparator.reverseOrder()))
                .limit(6)
                .toList();
        model.addAttribute("phongNoiBat", phongNoiBat);

        List<RoomTypeDTO> loaiPhongPhoBien = uniqueTypes.stream()
                .sorted(Comparator.comparing(RoomTypeDTO::getSoLuotDat).reversed()
                        .thenComparing(RoomTypeDTO::getDiemDanhGia, Comparator.reverseOrder())
                        .thenComparing(RoomTypeDTO::getTenLoaiPhong,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .limit(6)
                .toList();
        model.addAttribute("loaiPhongPhoBien", loaiPhongPhoBien);

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

        // Hiển thị mọi khuyến mãi công khai đang hiệu lực. Voucher riêng tư vẫn
        // không xuất hiện; voucher công khai được phép hiển thị để khách sao chép mã.
        Date now = new Date();
        List<Promotion> flashSales = promotionService.activePublicPromotions().stream()
                .sorted(Comparator.comparing(Promotion::getPriority,
                                Comparator.nullsFirst(Comparator.reverseOrder()))
                        .thenComparing(Promotion::getDiscountPercent,
                                Comparator.nullsFirst(Comparator.reverseOrder())))
                .toList();
        model.addAttribute("flashSales", flashSales);
        model.addAttribute("flashSale", flashSales.isEmpty() ? null : flashSales.get(0));

        // Đếm số phòng Trống (để hiển thị "chỉ còn X phòng")
        long soPhongTrong = allRooms.stream()
                .filter(r -> isVacant(r.getStatus()))
                .count();
        model.addAttribute("soPhongTrong", soPhongTrong);

        Promotion firstFlashSale = flashSales.isEmpty() ? null : flashSales.get(0);
        if (firstFlashSale != null && firstFlashSale.getEndDate() != null) {
            java.time.ZoneId hotelZone = java.time.ZoneId.of("Asia/Ho_Chi_Minh");
            long inclusiveEnd = firstFlashSale.getEndDate().toInstant()
                    .atZone(hotelZone)
                    .toLocalDate()
                    .plusDays(1)
                    .atStartOfDay(hotelZone)
                    .toInstant()
                    .minusMillis(1)
                    .toEpochMilli();
            long diff = inclusiveEnd - now.getTime();
            model.addAttribute("flashSaleSeconds", Math.max(diff / 1000, 0));
        } else {
            model.addAttribute("flashSaleSeconds", 0);
        }

        return "view/home/index";
    }

    private String normalizeDirection(String direction) {
        if (direction == null || direction.isBlank()) return "";
        String value = Normalizer.normalize(direction, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replace('Đ', 'D')
                .trim();
        if (value.contains("BIEN") || value.equals("SEA")) return "SEA";
        if (value.contains("THANH PHO") || value.equals("CITY")) return "CITY";
        if (value.contains("NUI") || value.equals("MOUNTAIN")) return "MOUNTAIN";
        if (value.contains("VUON") || value.equals("GARDEN")) return "GARDEN";
        if (value.contains("HO") || value.equals("LAKE")) return "LAKE";
        return value;
    }

    private String directionLabel(String direction) {
        return switch (normalizeDirection(direction)) {
            case "SEA" -> "Hướng biển";
            case "CITY" -> "Hướng thành phố";
            case "MOUNTAIN" -> "Hướng núi";
            case "GARDEN" -> "Hướng vườn";
            case "LAKE" -> "Hướng hồ";
            default -> direction == null || direction.isBlank() ? "Khác" : direction;
        };
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
    public String review(@RequestParam(name = "stars", defaultValue = "0") int stars,
            @RequestParam(name = "q", defaultValue = "") String keyword,
            Model model) {
        int starFilter = stars >= 1 && stars <= 5 ? stars : 0;
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        String keywordLower = normalizedKeyword.toLowerCase(Locale.ROOT);

        List<ReviewService.ReviewItem> allReviews = reviewService.latestReviews(10_000);
        long count5 = countReviewsByStars(allReviews, 5);
        long count4 = countReviewsByStars(allReviews, 4);
        long count3 = countReviewsByStars(allReviews, 3);
        long count2 = countReviewsByStars(allReviews, 2);
        long count1 = countReviewsByStars(allReviews, 1);
        double average = allReviews.stream()
                .mapToInt(ReviewService.ReviewItem::getStars)
                .average()
                .orElse(0.0);

        List<ReviewService.ReviewItem> filteredReviews = allReviews.stream()
                .filter(review -> starFilter == 0 || review.getStars() == starFilter)
                .filter(review -> keywordLower.isEmpty()
                        || containsIgnoreCase(review.getName(), keywordLower)
                        || containsIgnoreCase(review.getText(), keywordLower)
                        || containsIgnoreCase(review.getRoomLabel(), keywordLower))
                .toList();

        model.addAttribute("pageTitle", "Đánh giá khách hàng");
        model.addAttribute("avgRating", Math.round(average * 10.0) / 10.0);
        model.addAttribute("totalReviews", allReviews.size());
        model.addAttribute("count5", count5);
        model.addAttribute("count4", count4);
        model.addAttribute("count3", count3);
        model.addAttribute("count2", count2);
        model.addAttribute("count1", count1);
        model.addAttribute("starFilter", starFilter);
        model.addAttribute("keyword", normalizedKeyword);
        model.addAttribute("reviews", filteredReviews);
        model.addAttribute("filteredCount", filteredReviews.size());
        return "view/review/list";
    }

    private long countReviewsByStars(List<ReviewService.ReviewItem> reviews, int stars) {
        return reviews.stream().filter(review -> review.getStars() == stars).count();
    }

    private boolean containsIgnoreCase(String value, String normalizedKeyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(normalizedKeyword);
    }

    // ================================================================
    // HELPERS
    // ================================================================
    private String selectTypeImage(RoomType type, List<Room> rooms) {
        if (type != null && type.getImageUrl() != null && !type.getImageUrl().isBlank()) {
            return type.getImageUrl().trim();
        }
        return rooms.stream()
                .map(Room::getImageUrl)
                .filter(image -> image != null && !image.isBlank())
                .map(String::trim)
                .findFirst()
                .orElse("/images/a1.jpg");
    }

    private RoomTypeDTO toDTO(Room r, RoomType t) {
        RoomTypeDTO dto = new RoomTypeDTO();
        dto.setMaPhong(r.getId());
        dto.setSoPhong(r.getName());

        String typeName = (t != null && t.getName() != null) ? t.getName() : "Phòng";
        dto.setTenLoaiPhong(typeName);

        // Trang chủ giới thiệu LOẠI PHÒNG nên ưu tiên ảnh đại diện của loại phòng.
        // Chỉ dùng ảnh phòng vật lý làm dự phòng khi loại phòng chưa có ảnh.
        String img = t != null ? t.getImageUrl() : null;
        if (img == null || img.isBlank()) img = r.getImageUrl();
        if (img == null || img.isBlank()) img = "/images/a1.jpg";
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
