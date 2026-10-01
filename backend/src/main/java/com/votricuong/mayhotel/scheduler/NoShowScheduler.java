package com.votricuong.mayhotel.scheduler;

import com.votricuong.mayhotel.documents.BookingOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.Date;
import java.util.List;

@Component
public class NoShowScheduler {

    @Autowired
    private MongoTemplate mongoTemplate;

    // Chạy mỗi phút 1 lần để kiểm tra các phiếu đặt phòng quá hạn nhận phòng (No-show)
    @Scheduled(cron = "0 * * * * *")
    public void checkAndCancelOverdueBookings() {
        Date now = new Date();
        
        // Tìm các booking có trạng thái "Chờ nhận phòng" hoặc "Đã xác nhận cọc"
        // và autoCancelTime <= thời gian hiện tại
        Query query = new Query();
        query.addCriteria(Criteria.where("status").in("Chờ nhận phòng", "Đã xác nhận cọc"));
        query.addCriteria(Criteria.where("auto_cancel_time").lte(now));

        List<BookingOrder> overdueBookings = mongoTemplate.find(query, BookingOrder.class);

        if (!overdueBookings.isEmpty()) {
            System.out.println("⚠️ [No-Show AI] Phát hiện " + overdueBookings.size() + " đơn quá hạn. Đang tiến hành hủy tự động...");
            
            for (BookingOrder order : overdueBookings) {
                // Cập nhật trạng thái thành "Hủy do khách không đến"
                Update update = new Update().set("status", "Hủy do khách không đến");
                mongoTemplate.updateFirst(new Query(Criteria.where("id").is(order.getId())), update, BookingOrder.class);
                
                // TODO: Giải phóng tồn kho phòng trống
                System.out.println("✅ Đã tự động hủy đơn: " + order.getId() + " (Auto cancel time: " + order.getAutoCancelTime() + ")");
            }
        }
    }
}
