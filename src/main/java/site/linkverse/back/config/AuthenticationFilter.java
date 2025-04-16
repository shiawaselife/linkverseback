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
        String authHeader = request.headers().firstHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ServerResponse.status(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                    "success", false,
                    "message", "인증이 필요합니다"
                ));
        }
        
        String token = authHeader.substring(7);
        
        try {
            String userIdStr = jwtUtil.extractUserId(token);
            Long userId = Long.parseLong(userIdStr);
            
            return userRepository.findById(userId)
                .flatMap(user -> {
                    // 인증된 사용자 정보를 request attributes에 추가
                    request.attributes().put("userId", userId);
                    return next.handle(request);
                })
                .switchIfEmpty(ServerResponse.status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of(
                        "success", false,
                        "message", "사용자를 찾을 수 없습니다"
                    )));
        } catch (Exception e) {
            return ServerResponse.status(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                    "success", false,
                    "message", "유효하지 않은 토큰입니다"
                ));
        }
    }
}