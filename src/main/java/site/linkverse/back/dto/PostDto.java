package site.linkverse.back.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostDto {
    private String content;
    private String location;
    private String visibility;
    private List<String> hashtags;
    private List<MediaDto> media;
}

