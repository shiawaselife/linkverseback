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
public class FollowDto {
    private Long id;
    private Long followerId;
    private Long followingId;
    private UserDto follower;
    private UserDto following;
    private LocalDateTime createdAt;
}