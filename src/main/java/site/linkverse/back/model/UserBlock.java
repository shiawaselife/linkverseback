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
@Table("user_blocks")
public class UserBlock {
    @Id
    private Long id;
    @Column("blocker_id")
    private Long blockerId;
    @Column("blocked_id")
    private Long blockedId;
    @Column("created_at")
    private LocalDateTime createdAt;
}