package com.votricuong.mayhotel.controllers;

import com.votricuong.mayhotel.documents.Room;
import com.votricuong.mayhotel.documents.RoomType;
import com.votricuong.mayhotel.dto.RatingSnapshot;
import com.votricuong.mayhotel.services.ReviewService;
import com.votricuong.mayhotel.repositories.RoomRepository;
import com.votricuong.mayhotel.repositories.RoomTypeRepository;
import com.votricuong.mayhotel.documents.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class RoomController extends BaseController {

    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final ReviewService reviewService;

    public RoomController(RoomRepository roomRepository, RoomTypeRepository roomTypeRepository,
            ReviewService reviewService) {
        this.roomRepository = roomRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.reviewService = reviewService;
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
            @RequestParam(value = "priceRange", required = false, defaultValue = "all") String priceRange,
            @RequestParam(value = "sortBy", required = false, defaultValue = "default") String sortBy,
            Model model) {

        // 1. Fetch
        List<Room> allRooms = roomRepository.findAll();
        Map<Long, RoomType> typeMap = roomTypeRepository.findAll().stream()
                .collect(Collectors.toMap(RoomType::getId, t -> t));

        List<Room> filtered = new ArrayList<>(allRooms);

        // 2a. keyword — search room name + room type name
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim().toLowerCase();
            filtered = filtered.stream()
                    .filter(r -> {
                        RoomType t = r.getRoomTypeId() != null ? typeMap.get(r.getRoomTypeId()) : null;
                        String typeName = (t != null && t.getName() != null) ? t.getName().toLowerCase() : "";
                        String roomName = (r.getName() != null) ? r.getName().toLowerCase() : "";
                        return roomName.contains(kw) || typeName.contains(kw);
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
        if (guests != null && guests > 0) {
            filtered = filtered.stream()
                    .filter(r -> {
                        RoomType t = r.getRoomTypeId() != null ? typeMap.get(r.getRoomTypeId()) : null;
                        int max = (t != null && t.getMaxOccupancy() != null) ? t.getMaxOccupancy() : 2;
                        return max >= guests;
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

        // 4. Attach RoomType + fallback ảnh
        for (Room r : filtered) {
            RoomType t = (r.getRoomTypeId() != null) ? typeMap.get(r.getRoomTypeId()) : null;
            if (t != null) {
                r.setRoomType(t);
                if (r.getImageUrl() == null || r.getImageUrl().isBlank()) {
                    r.setImageUrl(t.getImageUrl());
                }
            }
        }

        // 5. Model
        model.addAttribute("rooms", filtered);
        model.addAttribute("roomTypes", roomTypeRepository.findAll());
        model.addAttribute("ratingMap", snap.getByRoomType());

        model.addAttribute("keyword", keyword);
        model.addAttribute("roomTypeId", roomTypeId);
        model.addAttribute("checkin", checkin);
        model.addAttribute("checkout", checkout);
        model.addAttribute("guests", guests);

        setPageTitle(model, "Danh sách phòng nghỉ");
        return "view/rooms/list";
    }

    // ================================================================
    // ROOM DETAIL — GET /rooms/detail/{id}
    // ================================================================
    @GetMapping("/rooms/detail/{id}")
    public String roomDetail(@PathVariable("id") Long id, Model model, HttpSession session) {

        Room room = roomRepository.findById(id).orElse(null);
        if (room == null) {
            return "redirect:/rooms";
        }

        // Attach RoomType
        if (room.getRoomTypeId() != null) {
            RoomType type = roomTypeRepository.findById(room.getRoomTypeId()).orElse(null);
            room.setRoomType(type);
            if ((room.getImageUrl() == null || room.getImageUrl().isBlank()) && type != null) {
                room.setImageUrl(type.getImageUrl());
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

        setPageTitle(model, "Chi tiết " + room.getName());
        return "view/rooms/detail";
    }
}