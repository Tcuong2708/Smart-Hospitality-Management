// seed_mock_data.js
db = db.getSiblingDB('MayHotelDB');

const now = new Date();

// 1. accounts
if (db.accounts.countDocuments() === 0) {
    db.accounts.insertOne({
        _id: NumberLong(1),
        username: "account_demo",
        password: "$2a$10$abcdefghijklmnopqrstuvwxyz123456",
        email: "account_demo@mayhotel.com",
        role_id: NumberLong(1),
        created_at: now,
        status: "ACTIVE"
    });
    print("Inserted mock data into 'accounts'");
}

// 2. backup_histories
if (db.backup_histories.countDocuments() === 0) {
    db.backup_histories.insertOne({
        _id: NumberLong(1),
        staff_id: NumberLong(1),
        file_name: "backup_2026_10_05.db",
        backup_date: now,
        size_mb: 15.5,
        status: "SUCCESS"
    });
    print("Inserted mock data into 'backup_histories'");
}

// 3. customer_types
if (db.customer_types.countDocuments() === 0) {
    db.customer_types.insertOne({
        _id: NumberLong(1),
        name: "VVIP",
        discount_rate: 15.0,
        required_points: "1000"
    });
    print("Inserted mock data into 'customer_types'");
}

// 4. gift_vouchers
if (db.gift_vouchers.countDocuments() === 0) {
    db.gift_vouchers.insertOne({
        _id: NumberLong(1),
        customer_id: NumberLong(1),
        value: 200000.0,
        expiry_date: new Date("2026-12-31T23:59:59Z"),
        status: "AVAILABLE"
    });
    print("Inserted mock data into 'gift_vouchers'");
}

// 5. loyal_customers
if (db.loyal_customers.countDocuments() === 0) {
    db.loyal_customers.insertOne({
        _id: NumberLong(1),
        customer_id: NumberLong(1),
        total_points: 1200,
        tier: "Bạch Kim"
    });
    print("Inserted mock data into 'loyal_customers'");
}

// 6. loyalty_policy
if (db.loyalty_policy.countDocuments() === 0) {
    db.loyalty_policy.insertOne({
        _id: "singleton",
        money_per_point: 100000.0,
        silver_threshold: 100,
        gold_threshold: 500,
        platinum_threshold: 1000
    });
    print("Inserted mock data into 'loyalty_policy'");
}

// 7. promotion_types
if (db.promotion_types.countDocuments() === 0) {
    db.promotion_types.insertOne({
        _id: NumberLong(1),
        name: "Khuyến mãi Lễ Tết",
        description: "Chương trình ưu đãi dịp Lễ và Tết"
    });
    print("Inserted mock data into 'promotion_types'");
}

// 8. promotions
if (db.promotions.countDocuments() === 0) {
    db.promotions.insertOne({
        _id: NumberLong(1),
        name: "Giảm 20% Chào Hè",
        discount_percent: 20.0,
        start_date: new Date("2026-06-01T00:00:00Z"),
        end_date: new Date("2026-08-31T23:59:59Z")
    });
    print("Inserted mock data into 'promotions'");
}

// 9. reviews
if (db.reviews.countDocuments() === 0) {
    db.reviews.insertOne({
        _id: NumberLong(1),
        booking_id: NumberLong(1),
        customer_id: NumberLong(1),
        rating: 5,
        content: "Dịch vụ phòng tuyệt vời, nhân viên chu đáo.",
        review_date: now,
        status: "Hiện",
        reply: "Cảm ơn quý khách đã đánh giá tốt!"
    });
    print("Inserted mock data into 'reviews'");
}

// 10. risk_alerts
if (db.risk_alerts.countDocuments() === 0) {
    db.risk_alerts.insertOne({
        _id: NumberLong(1),
        booking_id: NumberLong(1),
        risk_rate: 0.85,
        analyze_date: now,
        reason: "Khách hàng hủy phòng nhiều lần trong gian ngắn"
    });
    print("Inserted mock data into 'risk_alerts'");
}

// 11. role_details
if (db.role_details.countDocuments() === 0) {
    db.role_details.insertOne({
        _id: NumberLong(1),
        user_id: NumberLong(1),
        role_id: NumberLong(1)
    });
    print("Inserted mock data into 'role_details'");
}

// 12. service_tickets
if (db.service_tickets.countDocuments() === 0) {
    db.service_tickets.insertOne({
        _id: NumberLong(1),
        booking_id: NumberLong(1),
        service_id: NumberLong(1),
        quantity: 2,
        price: 500000.0
    });
    print("Inserted mock data into 'service_tickets'");
}

// 13. service_ticket_details
if (db.service_ticket_details.countDocuments() === 0) {
    db.service_ticket_details.insertOne({
        _id: NumberLong(1),
        service_ticket_id: NumberLong(1),
        service_id: NumberLong(1),
        quantity: 2,
        unit_price: 250000.0,
        total_price: 500000.0,
        order_time: now
    });
    print("Inserted mock data into 'service_ticket_details'");
}

print("=== MOCK DATA SEED COMPLETE ===");
