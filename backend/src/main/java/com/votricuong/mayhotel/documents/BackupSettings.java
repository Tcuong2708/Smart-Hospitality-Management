package com.votricuong.mayhotel.documents;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@NoArgsConstructor
@Document(collection = "backup_settings")
public class BackupSettings {
    @Id
    private String id = "singleton";
    private Boolean enabled = false;
    private String frequency = "DAILY";
    private Integer intervalHours = 24;
    private Date nextRunAt;
}
