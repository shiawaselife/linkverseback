package site.linkverse.back.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import site.linkverse.back.dto.ApiResponse;
import site.linkverse.back.dto.BookmarkDto;
import site.linkverse.back.dto.CollectionDto;
import site.linkverse.back.service.BookmarkService;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BookmarkHandler {
    private final BookmarkService bookmarkService;
    
    public Mono<ServerResponse> toggleBookmark(ServerRequest request) {
        Long userId = (Long) request.attributes().get("userId");
        Long postId = Long.parseLong(request.pathVariable("postId"));
        Long collectionId = request.queryParam("collectionId")
            .map(Long::parseLong)
            .orElse(null);
        
        return bookmarkService.toggleBookmark(userId, postId, collectionId)
            .flatMap(bookmarkDto -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                ApiResponse.<BookmarkDto>builder()
                    .success(true)
                    .message("북마크가 추가되었습니다")
                    .data(bookmarkDto)
                    .build()
            ))
            .switchIfEmpty(ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                ApiResponse.builder()
                    .success(true)
                    .message("북마크가 취소되었습니다")
                    .build()
            ))
            .onErrorResume(e -> ServerResponse.badRequest().bodyValue(
                ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            ));
    }
    
    public Mono<ServerResponse> getUserBookmarks(ServerRequest request) {
        Long userId = (Long) request.attributes().get("userId");
        int page = Integer.parseInt(request.queryParam("page").orElse("0"));
        int size = Integer.parseInt(request.queryParam("size").orElse("20"));
        
        return bookmarkService.getUserBookmarks(userId, page, size)
            .collectList()
            .flatMap(bookmarks -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                ApiResponse.<List<BookmarkDto>>builder()
                    .success(true)
                    .data(bookmarks)
                    .page(page)
                    .size(size)
                    .build()
            ));
    }
    
    public Mono<ServerResponse> createCollection(ServerRequest request) {
        Long userId = (Long) request.attributes().get("userId");
        
        return request.bodyToMono(CollectionDto.class)
            .flatMap(collectionDto -> bookmarkService.createCollection(userId, collectionDto.getName()))
            .flatMap(savedCollection -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                ApiResponse.<CollectionDto>builder()
                    .success(true)
                    .message("컬렉션이 생성되었습니다")
                    .data(savedCollection)
                    .build()
            ))
            .onErrorResume(e -> ServerResponse.badRequest().bodyValue(
                ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            ));
    }
    
    public Mono<ServerResponse> getUserCollections(ServerRequest request) {
        Long userId = (Long) request.attributes().get("userId");
        
        return bookmarkService.getUserCollections(userId)
            .collectList()
            .flatMap(collections -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                ApiResponse.<List<CollectionDto>>builder()
                    .success(true)
                    .data(collections)
                    .build()
            ));
    }
}