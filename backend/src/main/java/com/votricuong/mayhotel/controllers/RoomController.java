package com.votricuong.mayhotel.controllers;

import com.votricuong.mayhotel.documents.Room;
import com.votricuong.mayhotel.documents.RoomType;
import com.votricuong.mayhotel.dto.RatingSnapshot;
import com.votricuong.mayhotel.services.ReviewService;
import com.votricuong.mayhotel.repositories.RoomRepository;
import com.votricuong.mayhotel.repositories.RoomTypeRepository;
import com.votricuong.mayhotel.documents.User;
import com.votricuong.mayhotel.services.BookingAvailabilityService;
import com.votricuong.mayhotel.services.OnlineBookingPolicy;
import com.votricuong.mayhotel.services.PromotionService;
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
import java.util.Locale;
import java.util.Map;
import java.text.Normalizer;
import java.util.stream.Collectors;

@Controller
public class RoomController extends BaseController {

    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final ReviewService reviewService;
    private final BookingAvailabilityService bookingAvailabilityService;
    private final PromotionService promotionService;

    public RoomController(RoomRepository roomRepository, RoomTypeRepository roomTypeRepository,
            ReviewService reviewService, BookingAvailabilityService bookingAvailabilityService,
            PromotionService promotionService) {
        this.roomRepository = roomRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.reviewService = reviewService;
        this.bookingAvailabilityService = bookingAvailabilityService;
        this.promotionService = promotionService;
    }

    public record PublicPrice(double originalAmount, double discountAmount, double taxableAmount,
                              double taxAmount, double finalAmount, double originalPerNight,
                              double finalPerNight, String promotionName) {
        public boolean discounted() { return discountAmount > 0.0D; }
    }
    public record DirectionOption(String value, String label) {}
    private record RoomOfferKey(Long roomTypeId, String direction) {}
    private record RoomOffer(Room room, PublicPrice price, long availableCount, String directionLabel) {}

    // ================================================================
    // ROOM LIST — GET /rooms
    // ================================================================
    @GetMapping({ "/rooms", "/rooms/all" })
    public String listRooms(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "roomTypeId", required = false) Long roomTypeId,
            @RequestParam(value = "direction", required = false) String direction,
            @RequestParam(value = "checkin", required = false) String checkin,
            @RequestParam(value = "checkout", required = false) String checkout,
            @RequestParam(value = "guests", required = false) Integer guests,
            @RequestParam(value = "adults", required = false) Integer adults,
            @RequestParam(value = "children", required = false, defaultValue = "0") Integer children,
            @RequestParam(value = "infants", required = false, defaultValue = "0") Integer infants,
            @RequestParam(value = "priceRange", required = false, defaultValue = "all") String priceRange,
            @RequestParam(value = "sortBy", required = false, defaultValue = "default") String sortBy,
            @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "6") Integer size,
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

        // Public offers never expose physical room numbers.
        if (keyword != null && !keyword.isBlank()) {
            String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
            filtered = filtered.stream().filter(room -> {
                RoomType type = room.getRoomTypeId() == null ? null : typeMap.get(room.getRoomTypeId());
                return type != null && type.getName() != null
                        && type.getName().toLowerCase(Locale.ROOT).contains(normalizedKeyword);
            }).collect(Collectors.toList());
        }
        if (roomTypeId != null) {
            filtered = filtered.stream()
                    .filter(r -> roomTypeId.equals(r.getRoomTypeId()))
                    .collect(Collectors.toList());
        }
        String selectedDirection = normalizeDirection(direction);
        if (!selectedDirection.isBlank()) {
            filtered = filtered.stream()
                    .filter(r -> selectedDirection.equals(normalizeDirection(r.getViewDirection())))
                    .collect(Collectors.toList());
        }

        // Capacity comes from the room type.
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

        RatingSnapshot snap = reviewService.snapshot();
        // Attach room type and normalize the public category image path.
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

        Map<RoomOfferKey, List<Room>> grouped = new LinkedHashMap<>();
        for (Room room : filtered) {
            if (room.getRoomTypeId() == null) continue;
            grouped.computeIfAbsent(new RoomOfferKey(room.getRoomTypeId(),
                    normalizeDirection(room.getViewDirection())), ignored -> new ArrayList<>()).add(room);
        }

        int nights = Math.max(1, (int) java.time.temporal.ChronoUnit.DAYS.between(checkInDate, checkOutDate));
        List<RoomOffer> offers = new ArrayList<>();
        for (Map.Entry<RoomOfferKey, List<Room>> entry : grouped.entrySet()) {
            Room representative = entry.getValue().stream()
                    .min(Comparator.comparingDouble(r -> value(r.getPrice()))).orElse(null);
            if (representative == null) continue;
            representative.setStatus("Còn " + entry.getValue().size() + " phòng");
            PublicPrice price = publicPrice(representative.getPrice(), nights, totalGuests,
                    requestedIn, representative.getRoomTypeId());
            if (!matchesPriceRange(price.finalPerNight(), priceRange)) continue;
            offers.add(new RoomOffer(representative, price, entry.getValue().size(),
                    directionLabel(entry.getKey().direction())));
        }

        Comparator<RoomOffer> offerComparator = switch (sortBy) {
            case "priceAsc" -> Comparator.comparingDouble(o -> o.price().finalPerNight());
            case "priceDesc" -> Comparator.comparingDouble((RoomOffer o) -> o.price().finalPerNight()).reversed();
            default -> Comparator.comparingDouble((RoomOffer o) -> snap.rankOfType(o.room().getRoomTypeId())).reversed()
                    .thenComparing(o -> o.room().getRoomTypeId()).thenComparing(RoomOffer::directionLabel);
        };
        offers.sort(offerComparator);

        int pageSize = Math.max(1, Math.min(size == null ? 6 : size, 24));
        int totalResults = offers.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalResults / (double) pageSize));
        int currentPage = Math.max(0, Math.min(page == null ? 0 : page, totalPages - 1));
        int fromIndex = Math.min(currentPage * pageSize, totalResults);
        int toIndex = Math.min(fromIndex + pageSize, totalResults);
        List<RoomOffer> visibleOffers = offers.subList(fromIndex, toIndex);
        List<Room> publicRooms = visibleOffers.stream().map(RoomOffer::room).toList();
        Map<Long, PublicPrice> pricingByRoom = visibleOffers.stream().collect(Collectors.toMap(
                o -> o.room().getId(), RoomOffer::price, (a, b) -> a, LinkedHashMap::new));
        Map<Long, String> directionLabelsByRoom = visibleOffers.stream().collect(Collectors.toMap(
                o -> o.room().getId(), RoomOffer::directionLabel, (a, b) -> a, LinkedHashMap::new));
        Map<Long, Long> availableCountByRoom = visibleOffers.stream().collect(Collectors.toMap(
                o -> o.room().getId(), RoomOffer::availableCount, (a, b) -> a, LinkedHashMap::new));
        Map<String, String> directionOptions = new LinkedHashMap<>();
        availableRooms.stream().map(Room::getViewDirection).map(this::normalizeDirection)
                .filter(v -> !v.isBlank()).forEach(v -> directionOptions.putIfAbsent(v, directionLabel(v)));

        // 5. Model
        model.addAttribute("rooms", publicRooms);
        model.addAttribute("roomTypes", roomTypes);
        model.addAttribute("directionOptions", directionOptions.entrySet().stream()
                .map(e -> new DirectionOption(e.getKey(), e.getValue())).toList());
        model.addAttribute("ratingMap", snap.getByRoomType());
        model.addAttribute("pricingByRoom", pricingByRoom);
        model.addAttribute("directionLabelsByRoom", directionLabelsByRoom);
        model.addAttribute("availableCountByRoom", availableCountByRoom);
        model.addAttribute("totalResults", totalResults);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("pageSize", pageSize);

        model.addAttribute("keyword", keyword);
        model.addAttribute("roomTypeId", roomTypeId);
        model.addAttribute("direction", selectedDirection);
        model.addAttribute("priceRange", priceRange);
        model.addAttribute("sortBy", sortBy);
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
                             @RequestParam(value = "direction", required = false) String direction,
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
                List<Room> availableCategory = bookingAvailabilityService.findAvailableRooms(
                        room.getRoomTypeId(), requestedIn, requestedOut);
                String selectedDirection = normalizeDirection(direction);
                if (!selectedDirection.isBlank()) {
                    availableCategory = availableCategory.stream()
                            .filter(item -> selectedDirection.equals(normalizeDirection(item.getViewDirection())))
                            .toList();
                }
                boolean categoryAvailable = totalGuests <= capacity && !availableCategory.isEmpty();
                double categoryPrice = availableCategory.stream().map(Room::getPrice)
                        .filter(java.util.Objects::nonNull).mapToDouble(Double::doubleValue).min()
                        .orElse(value(room.getPrice()));
                room.setPrice(categoryPrice);
                room.setStatus(categoryAvailable ? "Còn phòng" : "Tạm hết phòng");
                model.addAttribute("checkin", in.toString());
                model.addAttribute("checkout", out.toString());
                model.addAttribute("adults", adults);
                model.addAttribute("children", children);
                model.addAttribute("infants", infants);
                model.addAttribute("direction", selectedDirection);
                model.addAttribute("directionLabel", selectedDirection.isBlank()
                        ? directionLabel(room.getViewDirection()) : directionLabel(selectedDirection));
                int nights = Math.max(1, (int) java.time.temporal.ChronoUnit.DAYS.between(in, out));
                model.addAttribute("publicPrice", publicPrice(categoryPrice, nights, totalGuests,
                        requestedIn, room.getRoomTypeId()));
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

    private PublicPrice publicPrice(Double nightlyPrice, int nights, int guests, Date stayDate,
                                    Long roomTypeId) {
        double original = value(nightlyPrice) * Math.max(1, nights);
        PromotionService.Quote quote = promotionService.bestQuote(original, 1, Math.max(1, guests),
                Math.max(1, nights), new Date(), stayDate, roomTypeId, null);
        OnlineBookingPolicy.PriceBreakdown price = OnlineBookingPolicy.priceBreakdown(
                original, 0D, quote.discount(), 0D);
        return new PublicPrice(price.subtotal(), price.discountAmount(), price.taxableAmount(),
                price.taxAmount(), price.totalAmount(), original / Math.max(1, nights),
                price.totalAmount() / Math.max(1, nights),
                quote.promotion() == null ? null : quote.promotion().getName());
    }

    private static double value(Double amount) {
        return amount == null || !Double.isFinite(amount) ? 0D : amount;
    }

    private boolean matchesPriceRange(double finalPrice, String priceRange) {
        return switch (priceRange == null ? "all" : priceRange) {
            case "under-500" -> finalPrice < 500_000D;
            case "500-1000" -> finalPrice >= 500_000D && finalPrice <= 1_000_000D;
            case "over-1000" -> finalPrice > 1_000_000D;
            default -> true;
        };
    }

    private String normalizeDirection(String direction) {
        if (direction == null || direction.isBlank()) return "";
        String value = Normalizer.normalize(direction, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replace('đ', 'd').replace('Đ', 'D')
                .trim().toUpperCase().replaceAll("[^A-Z0-9]+", "_");
        if (value.contains("BIEN") || value.contains("SEA")) return "SEA";
        if (value.contains("THANH_PHO") || value.contains("CITY")) return "CITY";
        if (value.contains("NUI") || value.contains("MOUNTAIN")) return "MOUNTAIN";
        if (value.contains("VUON") || value.contains("GARDEN")) return "GARDEN";
        if (value.contains("HO") || value.contains("LAKE")) return "LAKE";
        return value;
    }

    private String directionLabel(String direction) {
        return switch (normalizeDirection(direction)) {
            case "SEA" -> "Hướng biển";
            case "CITY" -> "Hướng thành phố";
            case "MOUNTAIN" -> "Hướng núi";
            case "GARDEN" -> "Hướng vườn";
            case "LAKE" -> "Hướng hồ";
            case "" -> "Chưa cập nhật hướng";
            default -> "Hướng khác";
        };
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
