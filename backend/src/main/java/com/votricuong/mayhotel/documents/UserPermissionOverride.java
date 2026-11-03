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
@Document(collection = "user_permission_overrides")
public class UserPermissionOverride {
    @Id
    private String id;
    @Field("user_id")
    private Long userId;
    @Field("permission_code")
    private String permissionCode;
    private String effect;
}
