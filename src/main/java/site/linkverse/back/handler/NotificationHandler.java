package site.linkverse.back.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import site.linkverse.back.dto.ApiResponse;
import site.linkverse.back.dto.NotificationDto;
import site.linkverse.back.dto.PostDto;
import site.linkverse.back.dto.UserDto;
import site.linkverse.back.service.NotificationService;
import site.linkverse.back.service.SearchService;

import java.util.List;
@Component
@RequiredArgsConstructor
public class NotificationHandler {
    private final NotificationService notificationService;

    public Mono<ServerResponse> getUserNotifications(ServerRequest request) {
        Long userId = (Long) request.attributes().get("userId");
        int page = Integer.parseInt(request.queryParam("page").orElse("0"));
        int size = Integer.parseInt(request.queryParam("size").orElse("20"));

        return notificationService.getUserNotifications(userId, page, size)
            .collectList()
            .flatMap(notifications -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                ApiResponse.<List<NotificationDto>>builder()
                    .success(true)
                    .data(notifications)
                    .page(page)
                    .size(size)
                    .build()
            ));
    }

    public Mono<ServerResponse> countUnreadNotifications(ServerRequest request) {
        Long userId = (Long) request.attributes().get("userId");

        return notificationService.countUnreadNotifications(userId)
            .flatMap(count -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                ApiResponse.<Long>builder()
                    .success(true)
                    .data(count)
                    .build()
            ));
    }

    public Mono<ServerResponse> markAsRead(ServerRequest request) {
        return request.bodyToMono(List.class)
                .flatMap(notificationIds -> notificationService.markAsRead(notificationIds))
                .then(ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                        ApiResponse.builder()
                                .success(true)
                                .message("알림을 읽음으로 표시했습니다")
                                .build()
                ))
                .onErrorResume(e -> {
                    // 명시적으로 Throwable로 처리하여 getMessage() 메서드 접근
                    String errorMessage = e instanceof Throwable ? ((Throwable) e).getMessage() : "Unknown error";
                    return ServerResponse.badRequest().bodyValue(
                            ApiResponse.builder()
                                    .success(false)
                                    .message(errorMessage)
                                    .build()
                    );
                });
    }
}