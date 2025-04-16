package site.linkverse.back.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import site.linkverse.back.enums.ReportStatus;
import site.linkverse.back.enums.ReportType;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("reports")
public class Report {
    @Id
    private Long id;
    @Column("reporter_id")
    private Long reporterId;
    @Column("reported_id")
    private Long reportedId;
    @Column("report_type")
    private ReportType reportType; // USER, POST, COMMENT
    @Column("reason")
    private String reason;
    @Column("status")
    private ReportStatus status; // PENDING, REVIEWED, REJECTED, ACTED
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
}