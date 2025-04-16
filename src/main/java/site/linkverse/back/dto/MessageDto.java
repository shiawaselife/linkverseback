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
public class MessageDto {
    private Long id;
    private Long senderId;
    private Long receiverId;
    private Long groupId;
    private UserDto sender;
    private UserDto receiver;
    private String content;
    private String mediaUrl;
    private boolean isRead;
    private LocalDateTime createdAt;
}