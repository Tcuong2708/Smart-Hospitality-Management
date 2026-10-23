package com.votricuong.mayhotel.documents;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
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
    private String operation;
    private String message;
    private Date completedAt;

    public BackupHistory(Long id, Long staffId, String fileName, Date backupDate,
                         Double sizeMb, String status) {
        this.id = id;
        this.staffId = staffId;
        this.fileName = fileName;
        this.backupDate = backupDate;
        this.sizeMb = sizeMb;
        this.status = status;
    }
}
