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
@Document(collection = "permission_audits")
public class PermissionAudit {
    @Id
    private Long id;
    @Field("actor_user_id")
    private Long actorUserId;
    @Field("target_role_id")
    private Long targetRoleId;
    @Field("target_user_id")
    private Long targetUserId;
    @Field("permission_code")
    private String permissionCode;
    @Field("old_effect")
    private String oldEffect;
    @Field("new_effect")
    private String newEffect;
    @Field("created_at")
    private Date createdAt;
}
