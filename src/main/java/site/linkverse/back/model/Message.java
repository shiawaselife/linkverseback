package site.linkverse.back.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("messages")
public class Message {
    @Id
    private Long id;
    @Column("sender_id")
    private Long senderId;
    @Column("receiver_id")
    private Long receiverId;
    @Column("group_id")
    private Long groupId; // 그룹 메시지의 경우
    @Column("content")
    private String content;
    @Column("media_url")
    private String mediaUrl;
    @Column("is_read")
    private boolean isRead;
    @Column("created_at")
    private LocalDateTime createdAt;
}