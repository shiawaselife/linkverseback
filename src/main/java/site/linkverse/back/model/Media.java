package site.linkverse.back.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import site.linkverse.back.enums.MediaType;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("media")
public class Media {
    @Id
    private Long id;
    @Column("post_id")
    private Long postId;
    @Column("media_type")
    private MediaType mediaType;
    @Column("url")
    private String url;
    @Column("created_at")
    private LocalDateTime createdAt;
}