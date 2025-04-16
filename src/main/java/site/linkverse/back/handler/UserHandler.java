package site.linkverse.back.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import site.linkverse.back.dto.*;
import site.linkverse.back.service.UserService;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UserHandler {
    private final UserService userService;
    
    public Mono<ServerResponse> register(ServerRequest request) {
        return request.bodyToMono(UserRegistrationDto.class)
            .flatMap(userService::registerUser)
            .flatMap(userDto -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                ApiResponse.<UserDto>builder()
                    .success(true)
                    .message("회원가입이 완료되었습니다")
                    .data(userDto)
                    .build()
            ))
            .onErrorResume(e -> ServerResponse.badRequest().bodyValue(
                ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            ));
    }
    
    public Mono<ServerResponse> login(ServerRequest request) {
        return request.bodyToMono(UserLoginDto.class)
            .flatMap(userService::login)
            .flatMap(authResponse -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                ApiResponse.<AuthResponse>builder()
                    .success(true)
                    .message("로그인이 완료되었습니다")
                    .data(authResponse)
                    .build()
            ))
            .onErrorResume(e -> ServerResponse.badRequest().bodyValue(
                ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            ));
    }
    
    public Mono<ServerResponse> getUserInfo(ServerRequest request) {
        Long userId = Long.parseLong(request.pathVariable("id"));
        
        return userService.getUserById(userId)
            .flatMap(userDto -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                ApiResponse.<UserDto>builder()
                    .success(true)
                    .data(userDto)
                    .build()
            ))
            .onErrorResume(e -> ServerResponse.badRequest().bodyValue(
                ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            ));
    }
    
    public Mono<ServerResponse> updateUser(ServerRequest request) {
        Long userId = Long.parseLong(request.pathVariable("id"));
        
        return request.bodyToMono(UserUpdateDto.class)
            .flatMap(userUpdateDto -> userService.updateUser(userId, userUpdateDto))
            .flatMap(userDto -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                ApiResponse.<UserDto>builder()
                    .success(true)
                    .message("사용자 정보가 업데이트되었습니다")
                    .data(userDto)
                    .build()
            ))
            .onErrorResume(e -> ServerResponse.badRequest().bodyValue(
                ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            ));
    }
    
    public Mono<ServerResponse> updatePassword(ServerRequest request) {
        Long userId = Long.parseLong(request.pathVariable("id"));
        
        return request.bodyToMono(PasswordUpdateDto.class)
            .flatMap(passwordUpdateDto -> userService.updatePassword(userId, passwordUpdateDto))
            .then(ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                ApiResponse.builder()
                    .success(true)
                    .message("비밀번호가 변경되었습니다")
                    .build()
            ))
            .onErrorResume(e -> ServerResponse.badRequest().bodyValue(
                ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            ));
    }
    
    public Mono<ServerResponse> searchUsers(ServerRequest request) {
        String keyword = request.queryParam("keyword").orElse("");
        int page = Integer.parseInt(request.queryParam("page").orElse("0"));
        int size = Integer.parseInt(request.queryParam("size").orElse("20"));
        
        return userService.searchUsers(keyword, page, size)
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
}