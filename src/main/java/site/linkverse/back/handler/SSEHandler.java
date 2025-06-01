package site.linkverse.back.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import site.linkverse.back.dto.NotificationDto;
import site.linkverse.back.service.SSENotificationService;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class SSEHandler {
    
    private final SSENotificationService sseNotificationService;
    private final ObjectMapper objectMapper;
    /*
    public Mono<ServerResponse> streamNotifications(ServerRequest request) {
        Long userId = (Long) request.attributes().get("userId");

        Flux<String> eventStream = sseNotificationService.createUserStream(userId)
                .map(this::formatSSEData)
                .mergeWith(createHeartbeat()) // 30초마다 heartbeat
                .onErrorResume(error -> {
                    log.error("SSE 스트림 에러 - 사용자 {}: {}", userId, error.getMessage());
                    return Flux.just("event: error\ndata: {\"message\":\"연결 오류\"}\n\n");
                })
                .doOnSubscribe(subscription -> {
                    log.info("SSE 연결 시작 - 사용자: {}", userId);
                    sseNotificationService.addConnection(userId);
                })
                .doOnCancel(() -> {
                    sseNotificationService.removeConnection(userId);
                });

        return ServerResponse.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .header("Cache-Control", "no-cache")
                .header("Connection", "keep-alive")
                .body(eventStream, String.class);
    }
     */

    public Mono<ServerResponse> streamNotifications(ServerRequest request) {
        // AuthenticationFilter에서 이미 인증을 처리했으므로 userId만 가져옴
        Long userId = (Long) request.attributes().get("userId");
        
        // 사용자별 SSE 스트림 생성
        Flux<String> eventStream = sseNotificationService.createUserStream(userId)
            .map(this::formatSSEData)
            .mergeWith(createHeartbeat()) // 30초마다 heartbeat 전송
            .onErrorResume(error -> {
                log.error("SSE 스트림 에러 - 사용자 {}: {}", userId, error.getMessage());
                return Flux.just("event: error\ndata: {\"message\":\"연결 오류가 발생했습니다\"}\n\n");
            })
            .doOnSubscribe(subscription -> {
                log.info("SSE 연결 시작 - 사용자: {}", userId);
                sseNotificationService.addConnection(userId);
            })
            .doOnCancel(() -> {
                log.info("SSE 연결 종료 - 사용자: {}", userId);
                sseNotificationService.removeConnection(userId);
            })
            .doFinally(signalType -> {
                log.info("SSE 스트림 종료 - 사용자: {}, 신호: {}", userId, signalType);
                sseNotificationService.removeConnection(userId);
            });
            
        return ServerResponse.ok()
            .contentType(MediaType.TEXT_EVENT_STREAM)
            .header("Cache-Control", "no-cache")
            .header("Connection", "keep-alive")
            .header("Access-Control-Allow-Origin", "*")
            .header("Access-Control-Allow-Headers", "Cache-Control")
            .body(eventStream, String.class);
    }

    private String formatSSEData(NotificationDto notification) {
        try {
            // ObjectMapper로 안전하게 JSON 직렬화
            String jsonData = objectMapper.writeValueAsString(notification);

            return "event: notification\n" +
                    "data: " + jsonData + "\n\n";

        } catch (Exception e) {
            log.error("SSE 데이터 포맷팅 실패", e);
            return "event: error\ndata: {\"message\":\"데이터 처리 오류\"}\n\n";
        }
    }

    private Flux<String> createHeartbeat() {
        return Flux.interval(Duration.ofSeconds(30))
                .map(tick -> "event: heartbeat\ndata: {\"timestamp\":\"" +
                        java.time.LocalDateTime.now() + "\"}\n\n");
    }
}