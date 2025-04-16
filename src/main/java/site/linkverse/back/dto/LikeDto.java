package site.linkverse.back.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import site.linkverse.back.enums.LikeTargetType;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LikeDto {
    private Long id;
    private Long userId;
    private UserDto user;
    private Long targetId;
    private LikeTargetType targetType;
    private LocalDateTime createdAt;
}