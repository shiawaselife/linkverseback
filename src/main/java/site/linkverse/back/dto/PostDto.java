package site.linkverse.back.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import site.linkverse.back.enums.VisibilityType;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostDto {
    private Long id;
    private Long userId;
    private UserDto user;
    private String content;
    private String location;
    private VisibilityType visibility;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<MediaDto> media;
    private List<String> hashtags;
    private Integer likesCount;
    private Integer commentsCount;
    private boolean isLiked;
    private boolean isBookmarked;
}