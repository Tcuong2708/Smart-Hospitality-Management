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
@Document(collection = "backup_histories")
public class BackupHistory {
    
    @Id
    private Long id;
    
    @Field("staff_id")
    private Long staffId;
    
    @Field("file_name")
    private String fileName;
    
    @Field("backup_date")
    private Date backupDate;
    
    @Field("size_mb")
    private Double sizeMb;
    
    private String status;
}
