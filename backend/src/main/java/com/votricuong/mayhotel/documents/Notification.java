package com.votricuong.mayhotel.documents;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notifications")
public class Notification {
    @Id
    private Long id;
    @Field("customer_id")
    private Long customerId;
    @Field("user_id")
    private Long userId;
    private String type;
    private String title;
    private String message;
    private String icon;
    private String link;
    @Field("is_read")
    private Boolean isRead;
    @Field("created_at")
    private Date createdAt;
    @Field("read_at")
    private Date readAt;
}
