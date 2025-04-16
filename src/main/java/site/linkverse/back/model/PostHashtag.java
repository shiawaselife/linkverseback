package site.linkverse.back.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("post_hashtags")
public class PostHashtag {
    @Id
    private Long id;
    @Column("post_id")
    private Long postId;
    @Column("hashtag_id")
    private Long hashtagId;
}