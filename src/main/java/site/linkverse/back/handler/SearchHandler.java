package site.linkverse.back.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import site.linkverse.back.dto.ApiResponse;
import site.linkverse.back.dto.PostDto;
import site.linkverse.back.dto.UserDto;
import site.linkverse.back.service.SearchService;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SearchHandler {
    private final SearchService searchService;

    public Mono<ServerResponse> searchUsers(ServerRequest request) {
        String keyword = request.queryParam("keyword").orElse("");
        int page = Integer.parseInt(request.queryParam("page").orElse("0"));
        int size = Integer.parseInt(request.queryParam("size").orElse("20"));

        return searchService.searchUsers(keyword, page, size)
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

    public Mono<ServerResponse> searchPosts(ServerRequest request) {
        String keyword = request.queryParam("keyword").orElse("");
        Long userId = (Long) request.attributes().get("userId");
        int page = Integer.parseInt(request.queryParam("page").orElse("0"));
        int size = Integer.parseInt(request.queryParam("size").orElse("20"));

        return searchService.searchPosts(keyword, userId, page, size)
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

    public Mono<ServerResponse> searchHashtags(ServerRequest request) {
        String keyword = request.queryParam("keyword").orElse("");
        int limit = Integer.parseInt(request.queryParam("limit").orElse("10"));

        return searchService.searchHashtags(keyword, limit)
                .collectList()
                .flatMap(hashtags -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                        ApiResponse.<List<String>>builder()
                                .success(true)
                                .data(hashtags)
                                .build()
                ));
    }

    public Mono<ServerResponse> getTrendingHashtags(ServerRequest request) {
        int limit = Integer.parseInt(request.queryParam("limit").orElse("10"));

        return searchService.getTrendingHashtags(limit)
                .collectList()
                .flatMap(hashtags -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                        ApiResponse.<List<String>>builder()
                                .success(true)
                                .data(hashtags)
                                .build()
                ));
    }
}