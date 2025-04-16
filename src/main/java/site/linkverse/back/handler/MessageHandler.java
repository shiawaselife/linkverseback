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

import java.util.List;

@Component
@RequiredArgsConstructor
public class MessageHandler {
    private final MessageService messageService;
    
    public Mono<ServerResponse> sendMessage(ServerRequest request) {
        Long senderId = (Long) request.attributes().get("userId");
        
        return request.bodyToMono(MessageCreateDto.class)
            .flatMap(messageCreateDto -> messageService.sendMessage(senderId, messageCreateDto))
            .flatMap(messageDto -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                ApiResponse.<MessageDto>builder()
                    .success(true)
                    .message("메시지가 전송되었습니다")
                    .data(messageDto)
                    .build()
            ))
            .onErrorResume(e -> ServerResponse.badRequest().bodyValue(
                ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            ));
    }
    
    public Mono<ServerResponse> getConversation(ServerRequest request) {
        Long userId = (Long) request.attributes().get("userId");
        Long otherUserId = Long.parseLong(request.pathVariable("otherUserId"));
        int page = Integer.parseInt(request.queryParam("page").orElse("0"));
        int size = Integer.parseInt(request.queryParam("size").orElse("20"));
        
        return messageService.getConversation(userId, otherUserId, page, size)
            .collectList()
            .flatMap(messages -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                ApiResponse.<List<MessageDto>>builder()
                    .success(true)
                    .data(messages)
                    .page(page)
                    .size(size)
                    .build()
            ));
    }
    
    public Mono<ServerResponse> getGroupMessages(ServerRequest request) {
        Long groupId = Long.parseLong(request.pathVariable("groupId"));
        int page = Integer.parseInt(request.queryParam("page").orElse("0"));
        int size = Integer.parseInt(request.queryParam("size").orElse("20"));
        
        return messageService.getGroupMessages(groupId, page, size)
            .collectList()
            .flatMap(messages -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                ApiResponse.<List<MessageDto>>builder()
                    .success(true)
                    .data(messages)
                    .page(page)
                    .size(size)
                    .build()
            ));
    }
    
    public Mono<ServerResponse> getRecentConversations(ServerRequest request) {
        Long userId = (Long) request.attributes().get("userId");
        int page = Integer.parseInt(request.queryParam("page").orElse("0"));
        int size = Integer.parseInt(request.queryParam("size").orElse("20"));
        
        return messageService.getRecentConversations(userId, page, size)
            .collectList()
            .flatMap(users -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                ApiResponse.<List<UserDto>>builder()
                    .success(true)
                    .data(users)
                    .page(page)
                    .size(size)
                    .build()
            ));
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