package com.votricuong.mayhotel.config;

import org.bson.Document;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

/**
 * Chuyển đổi tương thích các tên trường/collection cũ mà không làm mất dữ liệu.
 * Các thao tác đều idempotent để an toàn khi ứng dụng khởi động nhiều lần.
 */
@Component
public class MongoSchemaCompatibilityMigration implements ApplicationRunner {
    private final MongoTemplate mongoTemplate;

    public MongoSchemaCompatibilityMigration(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        migrateCustomerNationality();
        migrateGiftVouchers();
    }

    private void migrateCustomerNationality() {
        if (!mongoTemplate.collectionExists("customers")) return;
        Query legacy = Query.query(Criteria.where("quoc_tich").exists(true));
        for (Document customer : mongoTemplate.find(legacy, Document.class, "customers")) {
            Object nationality = customer.get("nationality");
            Object legacyValue = customer.get("quoc_tich");
            Update update = new Update().unset("quoc_tich");
            if ((nationality == null || String.valueOf(nationality).isBlank()) && legacyValue != null) {
                update.set("nationality", legacyValue);
            }
            mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(customer.get("_id"))),
                    update, "customers");
        }
    }

    private void migrateGiftVouchers() {
        if (!mongoTemplate.collectionExists("customer_promotions")) return;
        for (Document oldVoucher : mongoTemplate.findAll(Document.class, "customer_promotions")) {
            Object id = oldVoucher.get("_id");
            if (id == null || mongoTemplate.exists(Query.query(Criteria.where("_id").is(id)), "gift_voucher")) {
                continue;
            }
            mongoTemplate.insert(new Document(oldVoucher), "gift_voucher");
        }
    }
}
