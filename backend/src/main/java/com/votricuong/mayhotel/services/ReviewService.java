package com.votricuong.mayhotel.services;

import com.votricuong.mayhotel.documents.BookingDetail;
import com.votricuong.mayhotel.documents.BookingOrder;
import com.votricuong.mayhotel.documents.Customer;
import com.votricuong.mayhotel.documents.Invoice;
import com.votricuong.mayhotel.documents.Review;
import com.votricuong.mayhotel.documents.Room;
import com.votricuong.mayhotel.documents.User;
import com.votricuong.mayhotel.dto.RatingSnapshot;
import com.votricuong.mayhotel.dto.RatingStat;
import com.votricuong.mayhotel.repositories.BookingDetailRepository;
import com.votricuong.mayhotel.repositories.BookingOrderRepository;
import com.votricuong.mayhotel.repositories.CustomerRepository;
import com.votricuong.mayhotel.repositories.InvoiceRepository;
import com.votricuong.mayhotel.repositories.ReviewRepository;
import com.votricuong.mayhotel.repositories.RoomRepository;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Toàn bộ nghiệp vụ đánh giá: ai được đánh giá, lưu đánh giá, tổng hợp điểm
 * theo loại phòng.
 * Controller chỉ gọi service này, không tự xử lý nghiệp vụ.
 */
@Service
public class ReviewService {

    public static final String STATUS_VISIBLE = "Hiện";
    public static final String BOOKING_CHECKED_OUT = "Đã trả phòng";
    public static final String INVOICE_COMPLETED = "Completed";
    public static final String STATE_CAN_REVIEW = "CAN_REVIEW";
    public static final String STATE_REVIEWED = "REVIEWED";
    public static final String STATE_NONE = "NONE";

    private static final int MAX_CONTENT_LENGTH = 1000;

    private final ReviewRepository reviewRepository;
    private final BookingOrderRepository bookingOrderRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final RoomRepository roomRepository;
    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;
    private final SequenceGeneratorService sequenceGeneratorService;

    public ReviewService(ReviewRepository reviewRepository,
            BookingOrderRepository bookingOrderRepository,
            BookingDetailRepository bookingDetailRepository,
            RoomRepository roomRepository,
            CustomerRepository customerRepository,
            InvoiceRepository invoiceRepository,
            SequenceGeneratorService sequenceGeneratorService) {
        this.reviewRepository = reviewRepository;
        this.bookingOrderRepository = bookingOrderRepository;
        this.bookingDetailRepository = bookingDetailRepository;
        this.roomRepository = roomRepository;
        this.customerRepository = customerRepository;
        this.invoiceRepository = invoiceRepository;
        this.sequenceGeneratorService = sequenceGeneratorService;
    }

    // =====================================================================
    // 1. AI ĐƯỢC ĐÁNH GIÁ
    // =====================================================================

    public boolean ownsBooking(User user, BookingOrder order) {
        if (user == null || order == null || order.getCustomerId() == null) {
            return false;
        }
        Customer c = customerRepository.findById(order.getCustomerId()).orElse(null);
        if (c == null) {
            return false;
        }
        if (user.getId() != null && user.getId().equals(c.getUserId())) {
            return true;
        }
        if (notBlank(user.getPhone()) && user.getPhone().equals(c.getPhone())) {
            return true;
        }
        return notBlank(user.getEmail()) && user.getEmail().equalsIgnoreCase(c.getEmail());
    }

    public boolean canReview(BookingOrder order) {
        if (order == null) {
            return false;
        }
        if (BOOKING_CHECKED_OUT.equalsIgnoreCase(order.getStatus())) {
            return true;
        }
        List<Invoice> invoices = invoiceRepository.findByBookingId(order.getId());
        return invoices != null && invoices.stream()
                .anyMatch(i -> INVOICE_COMPLETED.equalsIgnoreCase(i.getInvoiceStatus()));
    }

    public Optional<Review> findReview(Long bookingId) {
        return reviewRepository.findByBookingId(bookingId);
    }

    public String reviewState(BookingOrder order) {
        if (order == null) {
            return STATE_NONE;
        }
        if (reviewRepository.findByBookingId(order.getId()).isPresent()) {
            return STATE_REVIEWED;
        }
        return canReview(order) ? STATE_CAN_REVIEW : STATE_NONE;
    }

    // =====================================================================
    // 2. LƯU ĐÁNH GIÁ
    // =====================================================================

    /**
     * Lưu đánh giá sau khi kiểm tra đủ điều kiện.
     * 
     * @return null nếu thành công, ngược lại là thông báo lỗi để hiện cho khách.
     */
    public String submit(User user, Long bookingId, Integer rating, String content) {
        if (user == null) {
            return "Vui lòng đăng nhập để đánh giá.";
        }
        BookingOrder order = bookingId == null ? null : bookingOrderRepository.findById(bookingId).orElse(null);
        if (order == null) {
            return "Không tìm thấy đơn đặt phòng.";
        }
        if (!ownsBooking(user, order)) {
            return "Bạn chỉ có thể đánh giá đơn đặt phòng của chính mình.";
        }
        if (reviewRepository.findByBookingId(bookingId).isPresent()) {
            return "Bạn đã đánh giá cho kỳ nghỉ này rồi. Cảm ơn bạn!";
        }
        if (!canReview(order)) {
            return "Chỉ có thể đánh giá sau khi đã hoàn tất lưu trú (đã trả phòng).";
        }
        if (rating == null || rating < 1 || rating > 5) {
            return "Vui lòng chọn số sao từ 1 đến 5.";
        }

        String text = content == null ? "" : content.trim();
        if (text.length() > MAX_CONTENT_LENGTH) {
            return "Nội dung đánh giá tối đa " + MAX_CONTENT_LENGTH + " ký tự.";
        }

        // Bộ đếm có thể thấp hơn id đã tồn tại (đã gặp ở hóa đơn) -> bỏ qua id trùng,
        // không ghi đè đánh giá cũ
        long id = sequenceGeneratorService.generateSequence("reviews_sequence");
        while (reviewRepository.existsById(id)) {
            id = sequenceGeneratorService.generateSequence("reviews_sequence");
        }

        Review review = new Review();
        review.setId(id);
        review.setBookingId(bookingId);
        review.setCustomerId(order.getCustomerId());
        review.setRating(rating);
        review.setContent(text);
        review.setReviewDate(new Date());
        review.setStatus(STATUS_VISIBLE); // quản lý có thể ẩn sau trong trang Quản lý đánh giá
        reviewRepository.save(review);
        return null;
    }

    /**
     * Đơn đã trả phòng, của người dùng này, có phòng thuộc loại phòng đang xem và
     * chưa đánh giá.
     * Dùng để hiện form đánh giá ngay trên trang chi tiết phòng. Không có -> null.
     */
    public Long findReviewableBookingId(User user, Long roomTypeId) {
        if (user == null || user.getId() == null || roomTypeId == null) {
            return null;
        }
        Set<Long> customerIds = new HashSet<>();
        customerRepository.findByUserId(user.getId()).forEach(c -> customerIds.add(c.getId()));
        if (notBlank(user.getPhone())) {
            customerRepository.findByPhone(user.getPhone()).forEach(c -> customerIds.add(c.getId()));
        }
        if (notBlank(user.getEmail())) {
            customerRepository.findByEmail(user.getEmail()).forEach(c -> customerIds.add(c.getId()));
        }
        if (customerIds.isEmpty()) {
            return null;
        }

        Set<Long> roomIdsOfType = roomRepository.findAll().stream()
                .filter(r -> r.getId() != null && roomTypeId.equals(r.getRoomTypeId()))
                .map(Room::getId)
                .collect(Collectors.toSet());

        List<BookingOrder> mine = bookingOrderRepository.findAll().stream()
                .filter(o -> o.getCustomerId() != null && customerIds.contains(o.getCustomerId()))
                .sorted(Comparator.comparing(BookingOrder::getBookingDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        for (BookingOrder o : mine) {
            List<BookingDetail> details = bookingDetailRepository.findByBookingId(o.getId());
            boolean sameType = details != null && details.stream()
                    .anyMatch(d -> d.getRoomId() != null && roomIdsOfType.contains(d.getRoomId()));
            if (sameType && STATE_CAN_REVIEW.equals(reviewState(o))) {
                return o.getId();
            }
        }
        return null;
    }

    // =====================================================================
    // 3. TỔNG HỢP ĐIỂM
    // =====================================================================

    /** Một đánh giá đã gắn với các loại phòng của đơn. */
    private static class Resolved {
        final Review review;
        final Set<Long> roomTypeIds;

        Resolved(Review review, Set<Long> roomTypeIds) {
            this.review = review;
            this.roomTypeIds = roomTypeIds;
        }
    }

    /**
     * Đánh giá đang hiển thị + loại phòng của từng đơn (3 truy vấn, không phụ thuộc
     * số đánh giá).
     */
    private List<Resolved> resolveVisible() {
        List<Review> visible = reviewRepository.findByStatus(STATUS_VISIBLE).stream()
                .filter(r -> r.getRating() != null && r.getBookingId() != null)
                .collect(Collectors.toList());
        if (visible.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Long> bookingIds = visible.stream().map(Review::getBookingId).collect(Collectors.toSet());

        Map<Long, Long> typeByRoom = new HashMap<>();
        for (Room r : roomRepository.findAll()) {
            if (r.getId() != null && r.getRoomTypeId() != null) {
                typeByRoom.put(r.getId(), r.getRoomTypeId());
            }
        }

        Map<Long, Set<Long>> typesByBooking = new HashMap<>();
        for (BookingDetail d : bookingDetailRepository.findAll()) {
            if (d.getBookingId() == null || d.getRoomId() == null || !bookingIds.contains(d.getBookingId())) {
                continue;
            }
            Long typeId = typeByRoom.get(d.getRoomId());
            if (typeId != null) {
                typesByBooking.computeIfAbsent(d.getBookingId(), k -> new HashSet<>()).add(typeId);
            }
        }

        List<Resolved> result = new ArrayList<>();
        for (Review r : visible) {
            result.add(new Resolved(r, typesByBooking.getOrDefault(r.getBookingId(), new HashSet<>())));
        }
        return result;
    }

    private static int clampStars(Integer rating) {
        return Math.max(1, Math.min(5, rating));
    }

    /** Điểm theo từng loại phòng + điểm chung. Gọi 1 lần cho mỗi request. */
    public RatingSnapshot snapshot() {
        List<Resolved> all = resolveVisible();
        if (all.isEmpty()) {
            return RatingSnapshot.empty();
        }

        Map<Long, int[]> acc = new HashMap<>(); // typeId -> {số đánh giá, tổng sao}
        int totalCount = 0;
        int totalSum = 0;
        for (Resolved x : all) {
            int stars = clampStars(x.review.getRating());
            totalCount++;
            totalSum += stars;
            for (Long typeId : x.roomTypeIds) { // mỗi đánh giá chỉ tính 1 lần cho mỗi loại phòng
                int[] a = acc.computeIfAbsent(typeId, k -> new int[2]);
                a[0]++;
                a[1] += stars;
            }
        }

        Map<Long, RatingStat> byType = new HashMap<>();
        acc.forEach((typeId, a) -> byType.put(typeId, new RatingStat(a[0], a[1])));
        return new RatingSnapshot(byType, new RatingStat(totalCount, totalSum));
    }

    // =====================================================================
    // 4. DANH SÁCH ĐÁNH GIÁ ĐỂ HIỂN THỊ
    // =====================================================================

    /** Một đánh giá đã chuẩn bị sẵn để template chỉ việc in ra. */
    public static class ReviewItem {
        private final String name;
        private final int stars;
        private final String text;
        private final String date;
        private final String reply;

        public ReviewItem(String name, int stars, String text, String date, String reply) {
            this.name = name;
            this.stars = stars;
            this.text = text;
            this.date = date;
            this.reply = reply;
        }

        public String getName() {
            return name;
        }

        public int getStars() {
            return stars;
        }

        public String getText() {
            return text;
        }

        public String getDate() {
            return date;
        }

        public String getReply() {
            return reply;
        }
    }

    /** Đánh giá mới nhất của một loại phòng (để hiện ở trang chi tiết phòng). */
    public List<ReviewItem> reviewsForRoomType(Long roomTypeId, int limit) {
        if (roomTypeId == null) {
            return new ArrayList<>();
        }
        List<Review> picked = resolveVisible().stream()
                .filter(x -> x.roomTypeIds.contains(roomTypeId))
                .map(x -> x.review)
                .collect(Collectors.toList());
        return toItems(picked, limit);
    }

    /** Đánh giá mới nhất của toàn khách sạn (trang "Khách hàng nói gì", API). */
    public List<ReviewItem> latestReviews(int limit) {
        List<Review> picked = resolveVisible().stream().map(x -> x.review).collect(Collectors.toList());
        return toItems(picked, limit);
    }

    private List<ReviewItem> toItems(List<Review> reviews, int limit) {
        List<Review> sorted = reviews.stream()
                .sorted(Comparator.comparing(Review::getReviewDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .collect(Collectors.toList());

        Set<Long> customerIds = sorted.stream().map(Review::getCustomerId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> names = new HashMap<>();
        customerRepository.findAllById(customerIds).forEach(c -> {
            if (notBlank(c.getFullName())) {
                names.put(c.getId(), c.getFullName());
            }
        });

        SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy");
        List<ReviewItem> items = new ArrayList<>();
        for (Review r : sorted) {
            items.add(new ReviewItem(
                    names.getOrDefault(r.getCustomerId(), "Khách hàng"),
                    clampStars(r.getRating()),
                    r.getContent() == null ? "" : r.getContent(),
                    r.getReviewDate() == null ? "" : fmt.format(r.getReviewDate()),
                    r.getReply()));
        }
        return items;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}