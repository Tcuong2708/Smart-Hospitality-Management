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
@Document(collection = "service_ticket_details")
public class ServiceTicketDetail {
    
    @Id
    private Long id;
    
    @Field("service_ticket_id")
    private Long serviceTicketId; // FK trỏ tới PhieuDichVu
    
    @Field("service_id")
    private Long serviceId; // FK trỏ tới DichVu
    
    @Field("quantity")
    private Integer quantity; // Số lượng
    
    @Field("unit_price")
    private Double unitPrice; // Đơn giá
    
    @Field("total_price")
    private Double totalPrice; // Thành tiền
    
    @Field("order_time")
    private Date orderTime; // Thời gian gọi
}
