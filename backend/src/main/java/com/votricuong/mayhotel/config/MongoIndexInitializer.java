package com.votricuong.mayhotel.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

/**
 * Tạo các chỉ mục phục vụ những luồng đọc thường xuyên trên MongoDB Atlas.
 * Mỗi chỉ mục có tên cố định nên thao tác này an toàn và không tạo bản sao
 * sau mỗi lần khởi động ứng dụng.
 */
@Slf4j
@Component
@Order(50)
public class MongoIndexInitializer implements ApplicationRunner {
    private final MongoTemplate mongoTemplate;

    @Value("${mayhotel.mongodb.ensure-indexes:true}")
    private boolean enabled;

    public MongoIndexInitializer(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) return;

        // Báo cáo doanh thu: lọc hóa đơn đã thanh toán trong khoảng thời gian.
        ensure("invoices", new Index().on("isPaid", Sort.Direction.ASC)
                .on("created_at", Sort.Direction.DESC).named("idx_invoice_paid_created"));
        ensure("invoices", new Index().on("isPaid", Sort.Direction.ASC)
                .on("payment_history.paymentDate", Sort.Direction.DESC)
                .named("idx_invoice_paid_payment_date"));
        ensure("invoices", new Index().on("booking_id", Sort.Direction.ASC)
                .named("idx_invoice_booking"));
        ensure("invoices", new Index().on("userId", Sort.Direction.ASC)
                .on("created_at", Sort.Direction.DESC).named("idx_invoice_user_created"));

        // Đặt/nhận/trả phòng và sơ đồ phòng theo thời điểm.
        ensure("booking_orders", new Index().on("status", Sort.Direction.ASC)
                .on("expected_in", Sort.Direction.ASC).on("expected_out", Sort.Direction.ASC)
                .named("idx_booking_status_stay"));
        ensure("booking_orders", new Index().on("customer_id", Sort.Direction.ASC)
                .on("booking_date", Sort.Direction.DESC).named("idx_booking_customer_date"));
        ensure("booking_orders", new Index().on("auto_cancel_time", Sort.Direction.ASC)
                .on("status", Sort.Direction.ASC).named("idx_booking_auto_cancel"));
        ensure("booking_details", new Index().on("booking_id", Sort.Direction.ASC)
                .named("idx_booking_detail_booking"));
        ensure("booking_details", new Index().on("room_id", Sort.Direction.ASC)
                .on("actual_in", Sort.Direction.ASC).on("actual_out", Sort.Direction.ASC)
                .named("idx_booking_detail_room_stay"));
        ensure("rooms", new Index().on("room_type_id", Sort.Direction.ASC)
                .on("status", Sort.Direction.ASC).named("idx_room_type_status"));

        // Tra cứu khách hàng, khuyến mãi và phiếu quà tặng.
        ensure("customers", new Index().on("user_id", Sort.Direction.ASC)
                .named("idx_customer_user"));
        ensure("customers", new Index().on("phone", Sort.Direction.ASC)
                .named("idx_customer_phone"));
        ensure("promotions", new Index().on("status", Sort.Direction.ASC)
                .on("start_date", Sort.Direction.ASC).on("end_date", Sort.Direction.ASC)
                .named("idx_promotion_status_period"));
        ensure("gift_voucher", new Index().on("customer_id", Sort.Direction.ASC)
                .on("status", Sort.Direction.ASC).named("idx_voucher_customer_status"));
        ensure("service_tickets", new Index().on("booking_id", Sort.Direction.ASC)
                .on("status", Sort.Direction.ASC).named("idx_service_booking_status"));
    }

    private void ensure(String collection, Index index) {
        try {
            mongoTemplate.indexOps(collection).ensureIndex(index);
        } catch (RuntimeException ex) {
            // Không làm ứng dụng ngừng chạy nếu tài khoản Atlas chỉ có quyền đọc/ghi dữ liệu.
            log.warn("Không thể bảo đảm chỉ mục MongoDB cho collection {}: {}", collection, ex.getMessage());
        }
    }
}
