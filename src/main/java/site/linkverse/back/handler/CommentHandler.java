package site.linkverse.back.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import site.linkverse.back.dto.ApiResponse;
import site.linkverse.back.dto.CommentCreateDto;
import site.linkverse.back.dto.CommentDto;
import site.linkverse.back.dto.CommentUpdateDto;
import site.linkverse.back.service.CommentService;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CommentHandler {
    private final CommentService commentService;
    
    public Mono<ServerResponse> createComment(ServerRequest request) {
        Long userId = (Long) request.attributes().get("userId");
        
        return request.bodyToMono(CommentCreateDto.class)
            .flatMap(commentCreateDto -> commentService.createComment(userId, commentCreateDto))
            .flatMap(commentDto -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                ApiResponse.<CommentDto>builder()
                    .success(true)
                    .message("댓글이 작성되었습니다")
                    .data(commentDto)
                    .build()
            ))
            .onErrorResume(e -> ServerResponse.badRequest().bodyValue(
                ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            ));
    }
    
    public Mono<ServerResponse> getPostComments(ServerRequest request) {
        Long postId = Long.parseLong(request.pathVariable("postId"));
        Long userId = (Long) request.attributes().get("userId");
        int page = Integer.parseInt(request.queryParam("page").orElse("0"));
        int size = Integer.parseInt(request.queryParam("size").orElse("20"));
        
        return commentService.getPostComments(postId, userId, page, size)
            .collectList()
            .flatMap(comments -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                ApiResponse.<List<CommentDto>>builder()
                    .success(true)
                    .data(comments)
                    .page(page)
                    .size(size)
                    .build()
            ));
    }
    
    public Mono<ServerResponse> updateComment(ServerRequest request) {
        Long commentId = Long.parseLong(request.pathVariable("id"));
        Long userId = (Long) request.attributes().get("userId");
        
        return request.bodyToMono(CommentUpdateDto.class)
            .flatMap(commentUpdateDto -> commentService.updateComment(commentId, userId, commentUpdateDto))
            .flatMap(commentDto -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                ApiResponse.<CommentDto>builder()
                    .success(true)
                    .message("댓글이 수정되었습니다")
                    .data(commentDto)
                    .build()
            ))
            .onErrorResume(e -> ServerResponse.badRequest().bodyValue(
                ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            ));
    }
    
    public Mono<ServerResponse> deleteComment(ServerRequest request) {
        Long commentId = Long.parseLong(request.pathVariable("id"));
        Long userId = (Long) request.attributes().get("userId");
        
        return commentService.deleteComment(commentId, userId)
            .then(ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                ApiResponse.builder()
                    .success(true)
                    .message("댓글이 삭제되었습니다")
                    .build()
            ))
            .onErrorResume(e -> ServerResponse.badRequest().bodyValue(
                ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            ));
    }
}