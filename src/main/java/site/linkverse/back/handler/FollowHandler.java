package site.linkverse.back.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import site.linkverse.back.dto.ApiResponse;
import site.linkverse.back.dto.FollowDto;
import site.linkverse.back.dto.UserDto;
import site.linkverse.back.service.FollowService;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FollowHandler {
    private final FollowService followService;
    
    public Mono<ServerResponse> toggleFollow(ServerRequest request) {
        Long followerId = (Long) request.attributes().get("userId");
        Long followingId = Long.parseLong(request.pathVariable("followingId"));
        
        return followService.toggleFollow(followerId, followingId)
            .flatMap(followDto -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                ApiResponse.<FollowDto>builder()
                    .success(true)
                    .message("팔로우가 완료되었습니다")
                    .data(followDto)
                    .build()
            ))
            .switchIfEmpty(ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                ApiResponse.builder()
                    .success(true)
                    .message("팔로우가 취소되었습니다")
                    .build()
            ))
            .onErrorResume(e -> ServerResponse.badRequest().bodyValue(
                ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            ));
    }
    
    public Mono<ServerResponse> getFollowers(ServerRequest request) {
        Long userId = Long.parseLong(request.pathVariable("userId"));
        int page = Integer.parseInt(request.queryParam("page").orElse("0"));
        int size = Integer.parseInt(request.queryParam("size").orElse("20"));
        
        return followService.getFollowers(userId, page, size)
            .collectList()
            .flatMap(followers -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                ApiResponse.<List<UserDto>>builder()
                    .success(true)
                    .data(followers)
                    .page(page)
                    .size(size)
                    .build()
            ));
    }
    
    public Mono<ServerResponse> getFollowing(ServerRequest request) {
        Long userId = Long.parseLong(request.pathVariable("userId"));
        int page = Integer.parseInt(request.queryParam("page").orElse("0"));
        int size = Integer.parseInt(request.queryParam("size").orElse("20"));
        
        return followService.getFollowing(userId, page, size)
            .collectList()
            .flatMap(following -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                ApiResponse.<List<UserDto>>builder()
                    .success(true)
                    .data(following)
                    .page(page)
                    .size(size)
                    .build()
            ));
    }
}