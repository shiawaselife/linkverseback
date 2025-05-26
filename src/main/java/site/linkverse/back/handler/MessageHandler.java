package site.linkverse.back.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import site.linkverse.back.dto.ApiResponse;
import site.linkverse.back.dto.MessageCreateDto;
import site.linkverse.back.dto.MessageDto;
import site.linkverse.back.dto.UserDto;
import site.linkverse.back.service.MessageService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class MessageHandler {
    private final MessageService messageService;
    private final DMWebSocketHandler dmWebSocketHandler; // WebSocket 핸들러 참조

    // 기존 메서드들은 그대로 유지...

    public Mono<ServerResponse> sendMessage(ServerRequest request) {
        Long senderId = (Long) request.attributes().get("userId");

        return request.bodyToMono(MessageCreateDto.class)
                .flatMap(messageCreateDto -> messageService.sendMessage(senderId, messageCreateDto))
                .flatMap(messageDto -> {
                    // WebSocket을 통한 실시간 알림은 DMWebSocketHandler에서 처리
                    // REST API는 기존과 동일하게 응답
                    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                            ApiResponse.<MessageDto>builder()
                                    .success(true)
                                    .message("메시지가 전송되었습니다")
                                    .data(messageDto)
                                    .build()
                    );
                })
                .onErrorResume(e -> ServerResponse.badRequest().bodyValue(
                        ApiResponse.builder()
                                .success(false)
                                .message(e.getMessage())
                                .build()
                ));
    }

    /**
     * 온라인 사용자 목록 조회 (새로 추가)
     */
    public Mono<ServerResponse> getOnlineUsers(ServerRequest request) {
        Set<Long> onlineUserIds = dmWebSocketHandler.getOnlineUsers();

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("onlineUsers", onlineUserIds);
        responseData.put("count", onlineUserIds.size());

        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                ApiResponse.<Map<String, Object>>builder()
                        .success(true)
                        .data(responseData)
                        .build()
        );
    }

    /**
     * 특정 사용자의 온라인 상태 확인 (새로 추가)
     */
    public Mono<ServerResponse> checkUserOnlineStatus(ServerRequest request) {
        Long userId = Long.parseLong(request.pathVariable("userId"));
        boolean isOnline = dmWebSocketHandler.isUserOnline(userId);

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("userId", userId);
        responseData.put("isOnline", isOnline);

        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                ApiResponse.<Map<String, Object>>builder()
                        .success(true)
                        .data(responseData)
                        .build()
        );
    }

    // 기존 메서드들...
    public Mono<ServerResponse> getConversation(ServerRequest request) {
        Long userId = (Long) request.attributes().get("userId");
        Long otherUserId = Long.parseLong(request.pathVariable("otherUserId"));
        int page = Integer.parseInt(request.queryParam("page").orElse("0"));
        int size = Integer.parseInt(request.queryParam("size").orElse("20"));

        return messageService.getConversation(userId, otherUserId, page, size)
                .collectList()
                .flatMap(messages -> {
                    // 상대방의 온라인 상태도 함께 전송
                    Map<String, Object> responseData = new HashMap<>();
                    responseData.put("messages", messages);
                    responseData.put("otherUserOnline", dmWebSocketHandler.isUserOnline(otherUserId));

                    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                            ApiResponse.<Map<String, Object>>builder()
                                    .success(true)
                                    .data(responseData)
                                    .build()
                    );
                });
    }

    public Mono<ServerResponse> getRecentConversations(ServerRequest request) {
        Long userId = (Long) request.attributes().get("userId");
        int page = Integer.parseInt(request.queryParam("page").orElse("0"));
        int size = Integer.parseInt(request.queryParam("size").orElse("20"));

        return messageService.getRecentConversations(userId, page, size)
                .collectList()
                .flatMap(users -> {
                    // 각 사용자의 온라인 상태도 함께 전송
                    List<Map<String, Object>> usersWithStatus = users.stream()
                            .map(user -> {
                                Map<String, Object> userWithStatus = new HashMap<>();
                                userWithStatus.put("user", user);
                                userWithStatus.put("isOnline", dmWebSocketHandler.isUserOnline(user.getId()));
                                return userWithStatus;
                            })
                            .toList();

                    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                            ApiResponse.<List<Map<String, Object>>>builder()
                                    .success(true)
                                    .data(usersWithStatus)
                                    .build()
                    );
                });
    }

    public Mono<ServerResponse> markAsRead(ServerRequest request) {
        return request.bodyToMono(List.class)
                .flatMap(messageIds -> messageService.markAsRead(messageIds))
                .then(ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                        ApiResponse.builder()
                                .success(true)
                                .message("메시지를 읽음으로 표시했습니다")
                                .build()
                ))
                .onErrorResume(e -> {
                    String errorMessage = e instanceof Exception ? ((Exception) e).getMessage() : "Unknown error occurred";
                    return ServerResponse.badRequest().bodyValue(
                            ApiResponse.builder()
                                    .success(false)
                                    .message(errorMessage)
                                    .build()
                    );
                });
    }
}