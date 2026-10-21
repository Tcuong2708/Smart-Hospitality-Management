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
@Document(collection = "service_tickets")
public class ServiceTicket {
    
    @Id
    private Long id;
    
    @Field("booking_id")
    private Long bookingId;
    
    @Field("service_id")
    private Long serviceId;
    
    private Integer quantity;
    
    private Double price;

    /**
     * Mã nhóm dùng để gom tất cả dịch vụ được khách xác nhận trong cùng một lần
     * đặt. Nhân viên dịch vụ dựa vào mã này để xử lý và in chung một phiếu.
     */
    @Field("request_group_id")
    private String requestGroupId;

    @Field("ordered_at")
    private Date orderedAt;

    /** Trạng thái xử lý trên ứng dụng nhân viên dịch vụ. */
    private String status;
}
