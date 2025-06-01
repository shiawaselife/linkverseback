package site.linkverse.back.config;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import site.linkverse.back.repository.UserRepository;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements HandlerFilterFunction<ServerResponse, ServerResponse> {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    public Mono<ServerResponse> filter(ServerRequest request, HandlerFunction<ServerResponse> next) {
        String token = extractToken(request);

        if (token == null) {
            return createUnauthorizedResponse("인증이 필요합니다");
        }

        try {
            String userIdStr = jwtUtil.extractUserId(token);
            Long userId = Long.parseLong(userIdStr);

            return userRepository.findById(userId)
                    .flatMap(user -> {
                        // 인증된 사용자 정보를 request attributes에 추가
                        request.attributes().put("userId", userId);
                        return next.handle(request);
                    })
                    .switchIfEmpty(createUnauthorizedResponse("사용자를 찾을 수 없습니다"));
        } catch (Exception e) {
            return createUnauthorizedResponse("유효하지 않은 토큰입니다");
        }
    }

    /**
     * 요청에서 JWT 토큰을 추출합니다.
     * 1. Authorization 헤더에서 Bearer 토큰 확인
     * 2. 쿼리 파라미터에서 token 확인 (SSE용)
     */
    private String extractToken(ServerRequest request) {
        // 1. Authorization 헤더에서 토큰 추출 (일반적인 REST API 요청)
        String authHeader = request.headers().firstHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // 2. 쿼리 파라미터에서 토큰 추출 (SSE 요청용)
        // EventSource는 헤더 설정이 제한적이므로 쿼리 파라미터로 토큰 전달
        return request.queryParam("token").orElse(null);
    }

    /**
     * 인증 실패 응답을 생성합니다.
     * SSE 요청인 경우와 일반 REST API 요청인 경우를 구분하여 처리합니다.
     */
    private Mono<ServerResponse> createUnauthorizedResponse(String message) {
        return Mono.fromCallable(() -> {
            // SSE 요청인지 확인 (Accept 헤더로 판단)
            return Map.of(
                    "success", false,
                    "message", message
            );
        }).flatMap(errorBody ->
                ServerResponse.status(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(errorBody)
        );
    }
}