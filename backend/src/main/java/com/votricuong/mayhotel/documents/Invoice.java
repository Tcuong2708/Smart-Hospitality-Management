package com.votricuong.mayhotel.documents;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "invoices")
public class Invoice {

    @Id
    private Long id;

    private String guestName;
    private String phone;
    private String address;
    
    private Date bookedAt;
    private Date checkInDate;
    private Date checkOutDate;
    
    private Double totalAmount;
    private Long userId;
    private Boolean isPaid;
    private Double surcharge;
    private Long roomId;
    private String invoiceStatus;
    private String note;
    private String paymentMethod;
    
    private List<RoomItem> roomItems;
    private List<ServiceItem> serviceItems;
    private List<SurchargeItem> surchargeItems;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoomItem {
        private Long roomId;
        private Integer quantity;
        private Double unitPrice;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceItem {
        private Long serviceId;
        private Integer quantity;
        private Double unitPrice;
        private String note;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SurchargeItem {
        private Long surchargeTypeId;
        private Integer quantity;
        private Double unitPrice;
        private String note;
    }
}
