package site.linkverse.back.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import site.linkverse.back.enums.NotificationType;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("notifications")
public class Notification {
    @Id
    private Long id;
    @Column("user_id")
    private Long userId;
    @Column("sender_id")
    private Long senderId;
    @Column("notification_type")
    private NotificationType notificationType;
    @Column("reference_id")
    private Long referenceId; // 관련 객체의 ID (게시물, 댓글 등)
    @Column("content")
    private String content;
    @Column("is_read")
    private boolean isRead;
    @Column("created_at")
    private LocalDateTime createdAt;
}