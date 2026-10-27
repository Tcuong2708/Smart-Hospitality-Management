package com.votricuong.mayhotel.controllers;

import com.votricuong.mayhotel.documents.Room;
import com.votricuong.mayhotel.documents.RoomType;
import com.votricuong.mayhotel.dto.RatingSnapshot;
import com.votricuong.mayhotel.services.ReviewService;
import com.votricuong.mayhotel.repositories.RoomRepository;
import com.votricuong.mayhotel.repositories.RoomTypeRepository;
import com.votricuong.mayhotel.documents.User;
import com.votricuong.mayhotel.services.BookingAvailabilityService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class RoomController extends BaseController {

    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final ReviewService reviewService;
    private final BookingAvailabilityService bookingAvailabilityService;

    public RoomController(RoomRepository roomRepository, RoomTypeRepository roomTypeRepository,
            ReviewService reviewService, BookingAvailabilityService bookingAvailabilityService) {
        this.roomRepository = roomRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.reviewService = reviewService;
        this.bookingAvailabilityService = bookingAvailabilityService;
    }

    // ================================================================
    // ROOM LIST — GET /rooms
    // ================================================================
    @GetMapping({ "/rooms", "/rooms/all" })
    public String listRooms(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "roomTypeId", required = false) Long roomTypeId,
            @RequestParam(value = "checkin", required = false) String checkin,
            @RequestParam(value = "checkout", required = false) String checkout,
            @RequestParam(value = "guests", required = false) Integer guests,
            @RequestParam(value = "adults", required = false) Integer adults,
            @RequestParam(value = "children", required = false, defaultValue = "0") Integer children,
            @RequestParam(value = "infants", required = false, defaultValue = "0") Integer infants,
            @RequestParam(value = "priceRange", required = false, defaultValue = "all") String priceRange,
            @RequestParam(value = "sortBy", required = false, defaultValue = "default") String sortBy,
            Model model) {

        // 1. Fetch
        List<Room> allRooms = roomRepository.findAll();
        List<RoomType> roomTypes = roomTypeRepository.findAll();
        Map<Long, RoomType> typeMap = roomTypes.stream()
                .collect(Collectors.toMap(RoomType::getId, t -> t));

        int adultCount = adults == null ? (guests == null ? 2 : guests) : adults;
        int childCount = children == null ? 0 : children;
        int infantCount = infants == null ? 0 : infants;
        int totalGuests;
        LocalDate checkInDate;
        LocalDate checkOutDate;
        try {
            totalGuests = com.votricuong.mayhotel.services.OnlineBookingPolicy
                    .totalGuests(adultCount, childCount, infantCount);
            checkInDate = checkin == null || checkin.isBlank() ? LocalDate.now() : LocalDate.parse(checkin);
            checkOutDate = checkout == null || checkout.isBlank() ? checkInDate.plusDays(1) : LocalDate.parse(checkout);
            com.votricuong.mayhotel.utils.InputValidator.stay(checkInDate, checkOutDate);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            adultCount = Math.max(1, adultCount);
            childCount = Math.max(0, childCount);
            infantCount = Math.max(0, infantCount);
            totalGuests = adultCount + childCount + infantCount;
            checkInDate = LocalDate.now();
            checkOutDate = checkInDate.plusDays(1);
        }
        checkin = checkInDate.toString();
        checkout = checkOutDate.toString();
        Date requestedIn = Date.from(checkInDate.atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant());
        Date requestedOut = Date.from(checkOutDate.atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant());
        List<Room> availableRooms = bookingAvailabilityService.findAvailableRooms(null, requestedIn, requestedOut);
        List<Room> filtered = new ArrayList<>(availableRooms);

        // 2a. Public search only uses the room type name; physical room codes
        // are assigned internally at check-in and must not be exposed here.
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim().toLowerCase();
            filtered = filtered.stream()
                    .filter(r -> {
                        RoomType t = r.getRoomTypeId() != null ? typeMap.get(r.getRoomTypeId()) : null;
                        String typeName = (t != null && t.getName() != null) ? t.getName().toLowerCase() : "";
                        return typeName.contains(kw);
                    })
                    .collect(Collectors.toList());
        }

        // 2b. room type
        if (roomTypeId != null) {
            filtered = filtered.stream()
                    .filter(r -> roomTypeId.equals(r.getRoomTypeId()))
                    .collect(Collectors.toList());
        }

        // 2c. price range
        if ("under-500".equals(priceRange)) {
            filtered = filtered.stream()
                    .filter(r -> r.getPrice() != null && r.getPrice() < 500_000)
                    .collect(Collectors.toList());
        } else if ("500-1000".equals(priceRange)) {
            filtered = filtered.stream()
                    .filter(r -> r.getPrice() != null && r.getPrice() >= 500_000 && r.getPrice() <= 1_000_000)
                    .collect(Collectors.toList());
        } else if ("over-1000".equals(priceRange)) {
            filtered = filtered.stream()
                    .filter(r -> r.getPrice() != null && r.getPrice() > 1_000_000)
                    .collect(Collectors.toList());
        }

        // 2d. guests — dùng maxOccupancy từ RoomType
        if (totalGuests > 0) {
            final int requestedGuests = totalGuests;
            filtered = filtered.stream()
                    .filter(r -> {
                        RoomType t = r.getRoomTypeId() != null ? typeMap.get(r.getRoomTypeId()) : null;
                        int max = (t != null && t.getMaxOccupancy() != null) ? t.getMaxOccupancy() : 2;
                        return max >= requestedGuests;
                    })
                    .collect(Collectors.toList());
        }

        // 3. Sort
        RatingSnapshot snap = reviewService.snapshot();
        switch (sortBy) {
            case "priceAsc":
                filtered.sort(Comparator.comparing(r -> r.getPrice() == null ? 0.0 : r.getPrice()));
                break;
            case "priceDesc":
                filtered.sort((a, b) -> Double.compare(
                        b.getPrice() == null ? 0.0 : b.getPrice(),
                        a.getPrice() == null ? 0.0 : a.getPrice()));
                break;
            default:
                // "Lựa chọn hàng đầu": loại phòng điểm (Bayesian) cao xếp trước; sort ổn định
                // nên bằng điểm thì giữ thứ tự cũ
                filtered.sort((a, b) -> Double.compare(
                        snap.rankOfType(b.getRoomTypeId()),
                        snap.rankOfType(a.getRoomTypeId())));
        }

        // 4. Attach RoomType and normalize the public category image path.
        for (Room r : filtered) {
            RoomType t = (r.getRoomTypeId() != null) ? typeMap.get(r.getRoomTypeId()) : null;
            if (t != null) {
                r.setRoomType(t);
                String publicImage = (t.getImageUrl() != null && !t.getImageUrl().isBlank())
                        ? t.getImageUrl()
                        : r.getImageUrl();
                r.setImageUrl(toPublicImageUrl(publicImage));
            }
        }

        Map<Long, Boolean> availabilityByType = availableRooms.stream()
                .filter(r -> r.getRoomTypeId() != null)
                .collect(Collectors.groupingBy(
                        Room::getRoomTypeId,
                        Collectors.reducing(false, r -> true, Boolean::logicalOr)));

        Map<Long, Room> publicRoomsByType = new LinkedHashMap<>();
        for (Room room : filtered) {
            if (room.getRoomTypeId() == null) {
                continue;
            }
            room.setStatus(Boolean.TRUE.equals(availabilityByType.get(room.getRoomTypeId()))
                    ? "Còn phòng"
                    : "Tạm hết phòng");
            publicRoomsByType.putIfAbsent(room.getRoomTypeId(), room);
        }
        List<Room> publicRooms = new ArrayList<>(publicRoomsByType.values());

        // 5. Model
        model.addAttribute("rooms", publicRooms);
        model.addAttribute("roomTypes", roomTypes);
        model.addAttribute("ratingMap", snap.getByRoomType());

        model.addAttribute("keyword", keyword);
        model.addAttribute("roomTypeId", roomTypeId);
        model.addAttribute("checkin", checkin);
        model.addAttribute("checkout", checkout);
        model.addAttribute("guests", guests);
        model.addAttribute("adults", adultCount);
        model.addAttribute("children", childCount);
        model.addAttribute("infants", infantCount);
        model.addAttribute("totalGuests", totalGuests);

        setPageTitle(model, "Danh sách phòng nghỉ");
        return "view/rooms/list";
    }

    // ================================================================
    // ROOM DETAIL — GET /rooms/detail/{id}
    // ================================================================
    @GetMapping("/rooms/detail/{id}")
    public String roomDetail(@PathVariable("id") Long id,
                             @RequestParam(value = "checkin", required = false) String checkin,
                             @RequestParam(value = "checkout", required = false) String checkout,
                             @RequestParam(value = "adults", required = false, defaultValue = "2") Integer adults,
                             @RequestParam(value = "children", required = false, defaultValue = "0") Integer children,
                             @RequestParam(value = "infants", required = false, defaultValue = "0") Integer infants,
                             Model model, HttpSession session) {

        Room room = roomRepository.findById(id).orElse(null);
        if (room == null) {
            return "redirect:/rooms";
        }

        // Attach RoomType
        if (room.getRoomTypeId() != null) {
            RoomType type = roomTypeRepository.findById(room.getRoomTypeId()).orElse(null);
            room.setRoomType(type);
            if (type != null) {
                String publicImage = (type.getImageUrl() != null && !type.getImageUrl().isBlank())
                        ? type.getImageUrl()
                        : room.getImageUrl();
                room.setImageUrl(toPublicImageUrl(publicImage));
                LocalDate in = checkin == null || checkin.isBlank() ? LocalDate.now() : LocalDate.parse(checkin);
                LocalDate out = checkout == null || checkout.isBlank() ? in.plusDays(1) : LocalDate.parse(checkout);
                Date requestedIn = Date.from(in.atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant());
                Date requestedOut = Date.from(out.atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant());
                int totalGuests = com.votricuong.mayhotel.services.OnlineBookingPolicy
                        .totalGuests(adults, children, infants);
                int capacity = type.getMaxOccupancy() == null ? 2 : type.getMaxOccupancy();
                boolean categoryAvailable = totalGuests <= capacity
                        && !bookingAvailabilityService.findAvailableRooms(
                                room.getRoomTypeId(), requestedIn, requestedOut).isEmpty();
                room.setStatus(categoryAvailable ? "Còn phòng" : "Tạm hết phòng");
                model.addAttribute("checkin", in.toString());
                model.addAttribute("checkout", out.toString());
                model.addAttribute("adults", adults);
                model.addAttribute("children", children);
                model.addAttribute("infants", infants);
            }
        }

        model.addAttribute("room", room);
        model.addAttribute("images", new ArrayList<>());
        model.addAttribute("reviews", new ArrayList<>());
        model.addAttribute("roomRating", reviewService.snapshot().getByRoomType().get(room.getRoomTypeId()));
        model.addAttribute("roomReviews", reviewService.reviewsForRoomType(room.getRoomTypeId(), 10));
        // Khách đã ở loại phòng này và chưa đánh giá -> id đơn để form đánh giá gửi lên
        model.addAttribute("reviewableBookingId",
                reviewService.findReviewableBookingId((User) session.getAttribute("user"), room.getRoomTypeId()));

        String publicName = room.getRoomType() != null && room.getRoomType().getName() != null
                ? room.getRoomType().getName()
                : "Loại phòng";
        setPageTitle(model, "Chi tiết " + publicName);
        return "view/rooms/detail";
    }

    private boolean isVacant(String status) {
        if (status == null) {
            return false;
        }
        String normalized = status.trim();
        return "Vacant".equalsIgnoreCase(normalized)
                || "Available".equalsIgnoreCase(normalized)
                || "Trống".equalsIgnoreCase(normalized)
                || "Còn phòng".equalsIgnoreCase(normalized)
                || "Phòng trống".equalsIgnoreCase(normalized);
    }

    private String toPublicImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return "/images/a1.jpg";
        }
        String normalized = imageUrl.trim().replace('\\', '/');
        if (normalized.startsWith("http://")
                || normalized.startsWith("https://")
                || normalized.startsWith("data:")) {
            return normalized;
        }
        if (normalized.startsWith("/")) {
            return normalized;
        }
        if (normalized.startsWith("images/")) {
            return "/" + normalized;
        }
        return "/images/" + normalized;
    }
}
