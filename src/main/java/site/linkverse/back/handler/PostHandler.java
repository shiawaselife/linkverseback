package site.linkverse.back.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import site.linkverse.back.dto.ApiResponse;
import site.linkverse.back.dto.PostCreateDto;
import site.linkverse.back.dto.PostDto;
import site.linkverse.back.dto.PostUpdateDto;
import site.linkverse.back.service.PostService;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PostHandler {
    private final PostService postService;
    
    public Mono<ServerResponse> createPost(ServerRequest request) {
        Long userId = (Long) request.attributes().get("userId");
        
        return request.bodyToMono(PostCreateDto.class)
            .flatMap(postCreateDto -> postService.createPost(userId, postCreateDto))
            .flatMap(postDto -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                ApiResponse.<PostDto>builder()
                    .success(true)
                    .message("게시물이 작성되었습니다")
                    .data(postDto)
                    .build()
            ))
            .onErrorResume(e -> ServerResponse.badRequest().bodyValue(
                ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            ));
    }
    
    public Mono<ServerResponse> getPost(ServerRequest request) {
        Long postId = Long.parseLong(request.pathVariable("id"));
        Long userId = (Long) request.attributes().get("userId");
        
        return postService.getPostById(postId, userId)
            .flatMap(postDto -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                ApiResponse.<PostDto>builder()
                    .success(true)
                    .data(postDto)
                    .build()
            ))
            .onErrorResume(e -> ServerResponse.badRequest().bodyValue(
                ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            ));
    }
    
    public Mono<ServerResponse> getFeedPosts(ServerRequest request) {
        Long userId = (Long) request.attributes().get("userId");
        int page = Integer.parseInt(request.queryParam("page").orElse("0"));
        int size = Integer.parseInt(request.queryParam("size").orElse("20"));
        
        return postService.getFeedPosts(userId, page, size)
            .collectList()
            .flatMap(posts -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                ApiResponse.<List<PostDto>>builder()
                    .success(true)
                    .data(posts)
                    .page(page)
                    .size(size)
                    .build()
            ));
    }
    
    public Mono<ServerResponse> getUserPosts(ServerRequest request) {
        Long targetUserId = Long.parseLong(request.pathVariable("userId"));
        Long currentUserId = (Long) request.attributes().get("userId");
        int page = Integer.parseInt(request.queryParam("page").orElse("0"));
        int size = Integer.parseInt(request.queryParam("size").orElse("20"));
        
        return postService.getUserPosts(targetUserId, currentUserId, page, size)
            .collectList()
            .flatMap(posts -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                ApiResponse.<List<PostDto>>builder()
                    .success(true)
                    .data(posts)
                    .page(page)
                    .size(size)
                    .build()
            ));
    }
    
    public Mono<ServerResponse> getHashtagPosts(ServerRequest request) {
        String hashtag = request.pathVariable("hashtag");
        Long userId = (Long) request.attributes().get("userId");
        int page = Integer.parseInt(request.queryParam("page").orElse("0"));
        int size = Integer.parseInt(request.queryParam("size").orElse("20"));
        
        return postService.getPostsByHashtag(hashtag, userId, page, size)
            .collectList()
            .flatMap(posts -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                ApiResponse.<List<PostDto>>builder()
                    .success(true)
                    .data(posts)
                    .page(page)
                    .size(size)
                    .build()
            ));
    }
    
    public Mono<ServerResponse> updatePost(ServerRequest request) {
        Long postId = Long.parseLong(request.pathVariable("id"));
        Long userId = (Long) request.attributes().get("userId");
        
        return request.bodyToMono(PostUpdateDto.class)
            .flatMap(postUpdateDto -> postService.updatePost(postId, userId, postUpdateDto))
            .flatMap(postDto -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                ApiResponse.<PostDto>builder()
                    .success(true)
                    .message("게시물이 수정되었습니다")
                    .data(postDto)
                    .build()
            ))
            .onErrorResume(e -> ServerResponse.badRequest().bodyValue(
                ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            ));
    }
    
    public Mono<ServerResponse> deletePost(ServerRequest request) {
        Long postId = Long.parseLong(request.pathVariable("id"));
        Long userId = (Long) request.attributes().get("userId");
        
        return postService.deletePost(postId, userId)
            .then(ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                ApiResponse.builder()
                    .success(true)
                    .message("게시물이 삭제되었습니다")
                    .build()
            ))
            .onErrorResume(e -> ServerResponse.badRequest().bodyValue(
                ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            ));
    }
    
    public Mono<ServerResponse> searchPosts(ServerRequest request) {
        String keyword = request.queryParam("keyword").orElse("");
        Long userId = (Long) request.attributes().get("userId");
        int page = Integer.parseInt(request.queryParam("page").orElse("0"));
        int size = Integer.parseInt(request.queryParam("size").orElse("20"));
        
        return postService.searchPosts(keyword, userId, page, size)
            .collectList()
            .flatMap(posts -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                ApiResponse.<List<PostDto>>builder()
                    .success(true)
                    .data(posts)
                    .page(page)
                    .size(size)
                    .build()
            ));
    }
}