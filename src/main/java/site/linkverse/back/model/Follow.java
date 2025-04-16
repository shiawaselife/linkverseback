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
@Table("follows")
public class Follow {
    @Id
    private Long id;
    @Column("follower_id")
    private Long followerId;
    @Column("following_id")
    private Long followingId;
    @Column("created_at")
    private LocalDateTime createdAt;
}