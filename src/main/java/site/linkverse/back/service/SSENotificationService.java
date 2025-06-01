package site.linkverse.back.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import site.linkverse.back.dto.NotificationDto;
import site.linkverse.back.enums.NotificationType;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class SSENotificationService {
    
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    
    // 사용자별 SSE 연결 관리
    private final Map<Long, Sinks.Many<NotificationDto>> userConnections = new ConcurrentHashMap<>();
    
    /**
     * 사용자별 SSE 스트림 생성
     */
    public Flux<NotificationDto> createUserStream(Long userId) {
        Sinks.Many<NotificationDto> sink = Sinks.many().multicast().onBackpressureBuffer();
        userConnections.put(userId, sink);
        
        return sink.asFlux()
            .timeout(Duration.ofHours(1)) // 5분 타임아웃
            .onErrorResume(error -> {
                log.warn("사용자 {} SSE 스트림 에러: {}", userId, error.getMessage());
                return Flux.empty();
            });
    }
    
    /**
     * 특정 사용자에게 실시간 알림 전송
     */
    public Mono<Void> sendNotificationToUser(Long userId, NotificationDto notification) {
        return Mono.fromRunnable(() -> {
            Sinks.Many<NotificationDto> userSink = userConnections.get(userId);
            if (userSink != null) {
                Sinks.EmitResult result = userSink.tryEmitNext(notification);
                if (result.isSuccess()) {
                    log.info("실시간 알림 전송 성공 - 사용자: {}, 알림 ID: {}", userId, notification.getId());
                } else {
                    log.warn("실시간 알림 전송 실패 - 사용자: {}, 결과: {}", userId, result);
                }
            } else {
                log.debug("사용자 {}는 현재 SSE 연결이 없습니다", userId);
            }
        });
    }
    
    /**
     * 알림 생성 및 실시간 전송
     */
    public Mono<NotificationDto> createAndSendNotification(
            Long userId, Long senderId, NotificationType type, Long referenceId, String content) {
        
        return notificationService.createNotification(userId, senderId, type, referenceId, content)
            .flatMap(notification -> {
                // 실시간 전송
                return sendNotificationToUser(userId, notification)
                    .thenReturn(notification);
            });
    }
    
    /**
     * 연결 추가
     */
    public void addConnection(Long userId) {
        log.info("SSE 연결 추가됨 - 사용자: {}", userId);
    }
    
    /**
     * 연결 제거
     */
    public void removeConnection(Long userId) {
        Sinks.Many<NotificationDto> sink = userConnections.remove(userId);
        if (sink != null) {
            sink.tryEmitComplete();
            log.info("SSE 연결 제거됨 - 사용자: {}", userId);
        }
    }
    
    /**
     * 현재 연결된 사용자 수
     */
    public int getActiveConnectionsCount() {
        return userConnections.size();
    }
    
    /**
     * 특정 사용자의 연결 상태 확인
     */
    public boolean isUserConnected(Long userId) {
        return userConnections.containsKey(userId);
    }

    @PreDestroy
    public void shutdownAllConnections() {
        log.info("서버 종료 중... 모든 SSE 연결을 정리합니다. 총 {}개 연결", userConnections.size());

        userConnections.forEach((userId, sink) -> {
            try {
                // 종료 메시지 전송
                sink.tryEmitNext(NotificationDto.builder()
                        .id(-1L)
                        .content("서버가 종료됩니다. 연결이 끊어집니다.")
                        .build());

                // 연결 종료
                sink.tryEmitComplete();
                log.info("사용자 {} SSE 연결 정리 완료", userId);
            } catch (Exception e) {
                log.warn("사용자 {} SSE 연결 정리 중 오류: {}", userId, e.getMessage());
            }
        });

        userConnections.clear();
        log.info("모든 SSE 연결 정리 완료");
    }

    /**
     * 수동으로 모든 연결 강제 종료
     */
    public void forceCloseAllConnections() {
        log.info("모든 SSE 연결을 강제로 종료합니다.");

        userConnections.forEach((userId, sink) -> {
            sink.tryEmitComplete();
        });

        userConnections.clear();
        log.info("강제 종료 완료. 연결된 사용자: 0명");
    }
}