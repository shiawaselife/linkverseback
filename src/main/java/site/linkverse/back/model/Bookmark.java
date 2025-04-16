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
@Table("bookmarks")
public class Bookmark {
    @Id
    private Long id;
    @Column("user_id")
    private Long userId;
    @Column("post_id")
    private Long postId;
    @Column("collection_id")
    private Long collectionId;
    @Column("created_at")
    private LocalDateTime createdAt;
}