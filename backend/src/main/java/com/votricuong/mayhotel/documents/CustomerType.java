package com.votricuong.mayhotel.documents;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "customer_types")
public class CustomerType {
    
    @Id
    private Long id; // Mã Hạng (MaHang)
    
    @Field("name")
    private String name; // Tên Hạng (TenHang)
    
    @Field("discount_rate")
    private Double discountRate; // Tỷ Lệ Giảm Giá (TyLeGiamGia)
    
    @Field("required_points")
    private String requiredPoints; // Điều kiện điểm (DieuKienDiem)
}
