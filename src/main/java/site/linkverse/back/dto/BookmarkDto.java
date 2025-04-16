package site.linkverse.back.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookmarkDto {
    private Long id;
    private Long userId;
    private Long postId;
    private PostDto post;
    private Long collectionId;
    private String collectionName;
    private LocalDateTime createdAt;
}