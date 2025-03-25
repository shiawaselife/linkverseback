package site.linkverse.back.handler;


import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import site.linkverse.back.dto.AuthRequest;
import site.linkverse.back.dto.UserDto;
import site.linkverse.back.service.UserService;

import java.util.HashMap;
import java.util.Objects;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@Component
@RequiredArgsConstructor
public class UserHandler {

    private final UserService userService;

    /**
     * 새 사용자 등록 처리
     */
    public Mono<ServerResponse> register(ServerRequest request) {
        // 요청 본문에서 UserDto 객체로 변환
        return request.bodyToMono(UserDto.class)
                .flatMap(userDto -> userService.registerUser(userDto))
                .flatMap(savedUser -> ServerResponse.ok()
                        .contentType(APPLICATION_JSON)
                        .bodyValue(savedUser));
    }

    /**
     * 로그인 처리 및 JWT 토큰 발급
     */
    public Mono<ServerResponse> login(ServerRequest request) {
        return request.bodyToMono(AuthRequest.class)
                .flatMap(authRequest -> userService.findByUsername(authRequest.getUsername())
                        .filter(user -> user.getPassword().equals(authRequest.getPassword()))
                        .flatMap(user -> {
                            var responseData = new HashMap<String, Object>();
                            responseData.put("username", user.getUsername());
                            responseData.put("success", true);

                            return ServerResponse.ok().contentType(APPLICATION_JSON).bodyValue(responseData);
                        })
                        .switchIfEmpty(Mono.defer(() -> {
                            var failResponse = new HashMap<String, Object>();
                            failResponse.put("success", false);

                            return ServerResponse.ok()
                                .contentType(APPLICATION_JSON)
                                .bodyValue(failResponse);
                        }))
                );


        /*
        return request.bodyToMono(AuthRequest.class)
                .flatMap(authRequest -> userService.findByEmail(authRequest.getEmail())
                        .filter(user -> passwordEncoder.matches(authRequest.getPassword(), user.getPassword()))
                        .flatMap(user -> {
                            // JWT 토큰 생성
                            String token = jwtService.generateToken(user.getEmail());
                            
                            // 응답 데이터 구성
                            var responseData = new HashMap<String, Object>();
                            responseData.put("token", token);
                            responseData.put("userId", user.getId());
                            
                            return ServerResponse.ok()
                                    .contentType(APPLICATION_JSON)
                                    .bodyValue(responseData);
                        })
                        .switchIfEmpty(ServerResponse.status(401).build()));

                 */
    }

    /**
     * 사용자 프로필 조회
     */
    public Mono<ServerResponse> getUserProfile(ServerRequest request) {
        // 경로 변수에서 사용자 ID 추출
        Long userId = Long.valueOf(request.pathVariable("id"));
        
        return userService.findById(userId)
                .flatMap(user -> ServerResponse.ok()
                        .contentType(APPLICATION_JSON)
                        .bodyValue(user))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    /**
     * 사용자 프로필 업데이트
     */
    public Mono<ServerResponse> updateProfile(ServerRequest request) {
        // 현재 인증된 사용자 이메일 가져오기
        String email = request.attribute("email")
                .map(Object::toString)
                .orElse("");
        
        return request.bodyToMono(UserDto.class)
                .flatMap(userDto -> userService.findByEmail(email)
                        .flatMap(user -> userService.updateUser(user.getId(), userDto)))
                .flatMap(updatedUser -> ServerResponse.ok()
                        .contentType(APPLICATION_JSON)
                        .bodyValue(updatedUser))
                .switchIfEmpty(ServerResponse.status(401).build());
    }

    /**
     * 이메일 인증 완료 처리
     */
    /*
    public Mono<ServerResponse> verifyEmail(ServerRequest request) {
        String token = request.pathVariable("token");
        // 실제 구현에서는 토큰을 검증하고 해당 이메일을 추출
        String email = jwtService.extractEmailFromVerificationToken(token);
        
        return userService.verifyEmail(email)
                .then(ServerResponse.ok().build());
    }

     */
}