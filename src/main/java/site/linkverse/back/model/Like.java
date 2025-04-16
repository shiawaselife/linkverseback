package site.linkverse.back.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import site.linkverse.back.enums.LikeTargetType;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("likes")
public class Like {
    @Id
    private Long id;
    @Column("user_id")
    private Long userId;
    @Column("target_id")
    private Long targetId;
    @Column("target_type")
    private LikeTargetType targetType; // POST, COMMENT
    @Column("created_at")
    private LocalDateTime createdAt;
}