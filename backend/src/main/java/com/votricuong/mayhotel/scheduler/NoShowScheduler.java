package com.votricuong.mayhotel.scheduler;

import com.votricuong.mayhotel.documents.BookingOrder;
import com.votricuong.mayhotel.documents.BookingDetail;
import com.votricuong.mayhotel.documents.Invoice;
import com.votricuong.mayhotel.documents.Room;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Date;
import java.util.List;
import com.votricuong.mayhotel.services.BookingCancellationService;

@Component
public class NoShowScheduler {

    private static final Logger log = LoggerFactory.getLogger(NoShowScheduler.class);

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private BookingCancellationService bookingCancellationService;

    // Chạy mỗi phút 1 lần để kiểm tra các phiếu đặt phòng quá hạn nhận phòng (No-show)
    @Scheduled(cron = "0 * * * * *")
    public void checkAndCancelOverdueBookings() {
        Date now = new Date();
        
        // Tìm các booking có trạng thái "Chờ nhận phòng" hoặc "Đã xác nhận cọc"
        // và autoCancelTime <= thời gian hiện tại
        Query query = new Query(Criteria.where("status")
                .in("Pending", "Đã xác nhận", "Chờ nhận phòng", "Đã xác nhận cọc", "Reserved"));

        List<BookingOrder> overdueBookings = mongoTemplate.find(query, BookingOrder.class);

        int cancelledCount = 0;
        for (BookingOrder order : overdueBookings) {
            Date cancelAt = resolveAutoCancelTime(order);
            if (cancelAt.after(now)
                    || Boolean.TRUE.equals(order.getIsFaceVerified())
                    || Boolean.TRUE.equals(order.getLateArrivalApproved())) {
                continue;
            }
            try {
                bookingCancellationService.cancel(order.getId(),
                        BookingCancellationService.CancellationType.NO_SHOW,
                        "Tự động hủy do quá hạn nhận phòng; phí phạt 100%, không hoàn lại");
                cancelledCount++;
                log.info("No-show AI: da tu dong huy don {} do qua han check-in luc {}, phat 100%",
                        order.getId(), cancelAt);
            } catch (IllegalStateException ex) {
                log.debug("Bo qua don {}: {}", order.getId(), ex.getMessage());
            }
        }
        if (cancelledCount > 0) {
            log.info("No-show AI: da huy {} don qua han.", cancelledCount);
        }
    }

    private Date resolveAutoCancelTime(BookingOrder order) {
        if (order.getAutoCancelTime() != null) {
            return order.getAutoCancelTime();
        }
        if (order.getExpectedIn() == null) {
            return new Date(Long.MAX_VALUE);
        }

        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTime(order.getExpectedIn());
        calendar.set(java.util.Calendar.HOUR_OF_DAY, "Cao".equals(order.getNoShowRiskLevel()) ? 14 : 18);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private void releaseReservedRoom(Long bookingId) {
        List<BookingDetail> details = mongoTemplate.find(
                new Query(Criteria.where("booking_id").is(bookingId)),
                BookingDetail.class);
        for (BookingDetail detail : details) {
            if (detail.getRoomId() == null) {
                continue;
            }
            mongoTemplate.updateFirst(
                    new Query(Criteria.where("_id").is(detail.getRoomId())
                            .and("status").in("Đã đặt", "Reserved")),
                    new Update().set("status", "Trống"),
                    Room.class);
        }
    }
}
