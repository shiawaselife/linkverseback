package site.linkverse.back.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import site.linkverse.back.dto.ApiResponse;
import site.linkverse.back.dto.LikeDto;
import site.linkverse.back.enums.LikeTargetType;
import site.linkverse.back.service.LikeService;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LikeHandler {
    private final LikeService likeService;
    
    public Mono<ServerResponse> toggleLike(ServerRequest request) {
        Long userId = (Long) request.attributes().get("userId");
        Long targetId = Long.parseLong(request.pathVariable("targetId"));
        LikeTargetType targetType = LikeTargetType.valueOf(request.pathVariable("targetType"));
        
        return likeService.toggleLike(userId, targetId, targetType)
                .flatMap(likeDto -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                        ApiResponse.<LikeDto>builder()
                                .success(true)
                                .message("좋아요가 추가되었습니다")
                                .data(likeDto)
                                .build()
                ))
                .switchIfEmpty(ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                        ApiResponse.builder()
                                .success(true)
                                .message("좋아요가 취소되었습니다")
                                .build()
                ))
                .onErrorResume(e -> ServerResponse.badRequest().bodyValue(
                        ApiResponse.builder()
                                .success(false)
                                .message(e.getMessage())
                                .build()
                ));
    }

    public Mono<ServerResponse> getLikes(ServerRequest request) {
        Long targetId = Long.parseLong(request.pathVariable("targetId"));
        LikeTargetType targetType = LikeTargetType.valueOf(request.pathVariable("targetType"));
        int page = Integer.parseInt(request.queryParam("page").orElse("0"));
        int size = Integer.parseInt(request.queryParam("size").orElse("20"));

        return likeService.getLikes(targetId, targetType, page, size)
                .collectList()
                .flatMap(likes -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                        ApiResponse.<List<LikeDto>>builder()
                                .success(true)
                                .data(likes)
                                .page(page)
                                .size(size)
                                .build()
                ));
    }
}