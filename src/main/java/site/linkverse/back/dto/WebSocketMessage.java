package site.linkverse.back.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import site.linkverse.back.dto.MessageDto;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebSocketMessage {
    private MessageType type;
    private MessageDto message;  // 기존 MessageDto 재사용
    private Long receiverId;
    private String error;
    private LocalDateTime timestamp;
    
    public enum MessageType {
        SEND_MESSAGE,    // 메시지 전송
        RECEIVE_MESSAGE, // 메시지 수신
        READ_RECEIPT,    // 읽음 확인
        TYPING_START,    // 타이핑 시작
        TYPING_STOP,     // 타이핑 중지
        USER_ONLINE,     // 사용자 온라인
        USER_OFFLINE,    // 사용자 오프라인
        ERROR           // 에러
    }
}