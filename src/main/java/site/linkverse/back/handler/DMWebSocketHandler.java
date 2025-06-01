package site.linkverse.back.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import site.linkverse.back.config.JwtUtil;
import site.linkverse.back.dto.MessageCreateDto;
import site.linkverse.back.dto.MessageDto;
import site.linkverse.back.dto.WebSocketMessage.MessageType;
import site.linkverse.back.repository.UserRepository;
import site.linkverse.back.service.MessageService;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class DMWebSocketHandler implements WebSocketHandler {

    private final ObjectMapper objectMapper;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final MessageService messageService;
    
    // 사용자별 WebSocket 세션 관리
    private final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    // 사용자별 메시지 스트림
    private final Map<Long, Sinks.Many<site.linkverse.back.dto.WebSocketMessage>> userSinks = new ConcurrentHashMap<>();
    // 타이핑 상태 관리
    private final Map<String, LocalDateTime> typingStatus = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        // JWT 토큰 검증
        String token = extractToken(session);
        if (token == null || !jwtUtil.validateToken(token)) {
            return session.close();
        }
        
        Long userId;
        try {
            userId = Long.parseLong(jwtUtil.extractUserId(token));
        } catch (Exception e) {
            log.error("토큰에서 사용자 ID 추출 실패: {}", e.getMessage());
            return session.close();
        }
        
        // 사용자 세션 등록
        userSessions.put(userId, session);
        userSinks.put(userId, Sinks.many().multicast().onBackpressureBuffer());
        
        log.info("사용자 {} WebSocket 연결됨", userId);
        
        // 온라인 상태 알림
        notifyUserOnline(userId);
        
        // 메시지 송신 스트림 (서버 → 클라이언트)
        Mono<Void> output = session.send(
            userSinks.get(userId).asFlux()
                .map(wsMessage -> {
                    try {
                        return session.textMessage(objectMapper.writeValueAsString(wsMessage));
                    } catch (JsonProcessingException e) {
                        log.error("메시지 직렬화 실패", e);
                        return session.textMessage("{}");
                    }
                })
        );
        
        // 메시지 수신 스트림 (클라이언트 → 서버)
        Mono<Void> input = session.receive()
            .doOnNext(message -> handleIncomingMessage(message, userId))
            .doFinally(signal -> handleDisconnect(userId))
            .then();
        
        return Mono.zip(input, output).then();
    }
    
    /**
     * 들어오는 메시지 처리
     */
    private void handleIncomingMessage(WebSocketMessage message, Long senderId) {
        try {
            site.linkverse.back.dto.WebSocketMessage wsMessage = 
                objectMapper.readValue(message.getPayloadAsText(), site.linkverse.back.dto.WebSocketMessage.class);
            
            switch (wsMessage.getType()) {
                case SEND_MESSAGE:
                    handleSendMessage(wsMessage, senderId);
                    break;
                case READ_RECEIPT:
                    handleReadReceipt(wsMessage, senderId);
                    break;
                case TYPING_START:
                    handleTypingStart(wsMessage, senderId);
                    break;
                case TYPING_STOP:
                    handleTypingStop(wsMessage, senderId);
                    break;
                default:
                    log.warn("알 수 없는 메시지 타입: {}", wsMessage.getType());
            }
        } catch (JsonProcessingException e) {
            log.error("메시지 파싱 실패: {}", e.getMessage());
            sendErrorMessage(senderId, "잘못된 메시지 형식입니다");
        }
    }
    
    /**
     * 메시지 전송 처리
     */
    private void handleSendMessage(site.linkverse.back.dto.WebSocketMessage wsMessage, Long senderId) {
        MessageDto messageData = wsMessage.getMessage();
        
        if (messageData == null || messageData.getReceiverId() == null || messageData.getContent() == null) {
            sendErrorMessage(senderId, "메시지 데이터가 불완전합니다");
            return;
        }
        
        // MessageCreateDto 생성 (기존 서비스 활용)
        MessageCreateDto createDto = MessageCreateDto.builder()
            .receiverId(messageData.getReceiverId())
            .content(messageData.getContent())
            .mediaUrl(messageData.getMediaUrl())
            .build();
        
        // 기존 MessageService를 통해 메시지 저장
        messageService.sendMessage(senderId, createDto)
            .subscribe(
                savedMessage -> {
                    // 송신자에게 전송 완료 알림
                    site.linkverse.back.dto.WebSocketMessage responseToSender = 
                        site.linkverse.back.dto.WebSocketMessage.builder()
                            .type(MessageType.SEND_MESSAGE)
                            .message(savedMessage)
                            .timestamp(LocalDateTime.now())
                            .build();
                    
                    sendMessageToUser(senderId, responseToSender);
                    
                    // 수신자에게 새 메시지 알림
                    site.linkverse.back.dto.WebSocketMessage responseToReceiver = 
                        site.linkverse.back.dto.WebSocketMessage.builder()
                            .type(MessageType.RECEIVE_MESSAGE)
                            .message(savedMessage)
                            .timestamp(LocalDateTime.now())
                            .build();
                    
                    sendMessageToUser(savedMessage.getReceiverId(), responseToReceiver);
                    
                    log.info("실시간 메시지 전송 완료: {} → {}", senderId, savedMessage.getReceiverId());
                },
                error -> {
                    log.error("메시지 저장 실패: {}", error.getMessage());
                    sendErrorMessage(senderId, "메시지 전송에 실패했습니다: " + error.getMessage());
                }
            );
    }
    
    /**
     * 읽음 확인 처리
     */
    private void handleReadReceipt(site.linkverse.back.dto.WebSocketMessage wsMessage, Long userId) {
        if (wsMessage.getMessage() != null && wsMessage.getMessage().getId() != null) {
            Long messageId = wsMessage.getMessage().getId();

            messageService.markAsRead(java.util.Collections.singletonList(messageId))
                    .subscribe(
                            success -> {
                                MessageDto messageDto = wsMessage.getMessage();

                                if (messageDto.getSenderId() != null && !messageDto.getSenderId().equals(userId)) {
                                    // 명시적 타입 지정
                                    site.linkverse.back.dto.WebSocketMessage readReceiptMessage =
                                            site.linkverse.back.dto.WebSocketMessage.builder()
                                                    .type(MessageType.READ_RECEIPT)
                                                    .message((MessageDto) wsMessage.getMessage()) // 명시적 캐스팅
                                                    .timestamp(LocalDateTime.now())
                                                    .build();

                                    sendMessageToUser(messageDto.getSenderId(), readReceiptMessage);
                                    log.info("읽음 확인 처리: 메시지 ID {}", messageId);
                                }
                            },
                            error -> log.error("읽음 확인 실패: {}", error.getMessage())
                    );
        }
    }
    
    /**
     * 타이핑 시작 처리
     */
    private void handleTypingStart(site.linkverse.back.dto.WebSocketMessage wsMessage, Long senderId) {
        Long receiverId = wsMessage.getReceiverId();
        if (receiverId != null) {
            String typingKey = senderId + "-" + receiverId;
            typingStatus.put(typingKey, LocalDateTime.now());
            
            site.linkverse.back.dto.WebSocketMessage typingMessage = 
                site.linkverse.back.dto.WebSocketMessage.builder()
                    .type(MessageType.TYPING_START)
                    .receiverId(senderId) // 타이핑하는 사람의 ID
                    .timestamp(LocalDateTime.now())
                    .build();
            
            sendMessageToUser(receiverId, typingMessage);
            
            // 5초 후 자동으로 타이핑 중지
            Mono.delay(java.time.Duration.ofSeconds(5))
                .subscribe(tick -> {
                    if (typingStatus.containsKey(typingKey)) {
                        handleTypingStop(wsMessage, senderId);
                    }
                });
        }
    }
    
    /**
     * 타이핑 중지 처리
     */
    private void handleTypingStop(site.linkverse.back.dto.WebSocketMessage wsMessage, Long senderId) {
        Long receiverId = wsMessage.getReceiverId();
        if (receiverId != null) {
            String typingKey = senderId + "-" + receiverId;
            typingStatus.remove(typingKey);
            
            site.linkverse.back.dto.WebSocketMessage typingMessage = 
                site.linkverse.back.dto.WebSocketMessage.builder()
                    .type(MessageType.TYPING_STOP)
                    .receiverId(senderId) // 타이핑 중지한 사람의 ID
                    .timestamp(LocalDateTime.now())
                    .build();
            
            sendMessageToUser(receiverId, typingMessage);
        }
    }
    
    /**
     * 특정 사용자에게 메시지 전송
     */
    private void sendMessageToUser(Long userId, site.linkverse.back.dto.WebSocketMessage message) {
        Sinks.Many<site.linkverse.back.dto.WebSocketMessage> userSink = userSinks.get(userId);
        if (userSink != null) {
            userSink.tryEmitNext(message);
        } else {
            log.debug("사용자 {}는 현재 오프라인 상태입니다", userId);
        }
    }
    
    /**
     * 에러 메시지 전송
     */
    private void sendErrorMessage(Long userId, String errorMessage) {
        site.linkverse.back.dto.WebSocketMessage errorMsg = 
            site.linkverse.back.dto.WebSocketMessage.builder()
                .type(MessageType.ERROR)
                .error(errorMessage)
                .timestamp(LocalDateTime.now())
                .build();
        
        sendMessageToUser(userId, errorMsg);
    }
    
    /**
     * 사용자 온라인 상태 알림
     */
    private void notifyUserOnline(Long userId) {
        site.linkverse.back.dto.WebSocketMessage onlineMessage = 
            site.linkverse.back.dto.WebSocketMessage.builder()
                .type(MessageType.USER_ONLINE)
                .receiverId(userId)
                .timestamp(LocalDateTime.now())
                .build();
        
        // 모든 연결된 사용자에게 알림 (친구 목록이 있다면 해당 사용자들에게만)
        userSinks.keySet().forEach(connectedUserId -> {
            if (!connectedUserId.equals(userId)) {
                sendMessageToUser(connectedUserId, onlineMessage);
            }
        });
    }
    
    /**
     * 사용자 연결 해제 처리
     */
    private void handleDisconnect(Long userId) {
        userSessions.remove(userId);
        
        Sinks.Many<site.linkverse.back.dto.WebSocketMessage> userSink = userSinks.remove(userId);
        if (userSink != null) {
            userSink.tryEmitComplete();
        }
        
        // 타이핑 상태 정리
        typingStatus.entrySet().removeIf(entry -> entry.getKey().startsWith(userId + "-"));
        
        log.info("사용자 {} WebSocket 연결 해제됨", userId);
        
        // 오프라인 상태 알림
        site.linkverse.back.dto.WebSocketMessage offlineMessage = 
            site.linkverse.back.dto.WebSocketMessage.builder()
                .type(MessageType.USER_OFFLINE)
                .receiverId(userId)
                .timestamp(LocalDateTime.now())
                .build();
        
        userSinks.keySet().forEach(connectedUserId -> 
            sendMessageToUser(connectedUserId, offlineMessage));
    }
    
    /**
     * URL에서 JWT 토큰 추출
     */
    private String extractToken(WebSocketSession session) {
        String query = session.getHandshakeInfo().getUri().getQuery();
        if (query != null) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2 && "token".equals(keyValue[0])) {
                    return keyValue[1];
                }
            }
        }
        return null;
    }
    
    /**
     * 현재 온라인 사용자 목록 조회
     */
    public java.util.Set<Long> getOnlineUsers() {
        return userSessions.keySet();
    }
    
    /**
     * 특정 사용자 온라인 상태 확인
     */
    public boolean isUserOnline(Long userId) {
        return userSessions.containsKey(userId);
    }
}
