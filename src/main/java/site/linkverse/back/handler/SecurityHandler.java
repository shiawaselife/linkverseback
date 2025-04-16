package site.linkverse.back.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import site.linkverse.back.dto.ApiResponse;
import site.linkverse.back.dto.BlockUserDto;
import site.linkverse.back.dto.ReportDto;
import site.linkverse.back.dto.UserDto;
import site.linkverse.back.service.SecurityService;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SecurityHandler {
    private final SecurityService securityService;
    
    public Mono<ServerResponse> blockUser(ServerRequest request) {
        Long blockerId = (Long) request.attributes().get("userId");
        
        return request.bodyToMono(BlockUserDto.class)
            .flatMap(blockUserDto -> securityService.blockUser(blockerId, blockUserDto))
            .then(ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                ApiResponse.builder()
                    .success(true)
                    .message("사용자가 차단되었습니다")
                    .build()
            ))
            .onErrorResume(e -> ServerResponse.badRequest().bodyValue(
                ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            ));
    }
    
    public Mono<ServerResponse> unblockUser(ServerRequest request) {
        Long blockerId = (Long) request.attributes().get("userId");
        Long blockedId = Long.parseLong(request.pathVariable("blockedId"));
        
        return securityService.unblockUser(blockerId, blockedId)
            .then(ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                ApiResponse.builder()
                    .success(true)
                    .message("사용자 차단이 해제되었습니다")
                    .build()
            ))
            .onErrorResume(e -> ServerResponse.badRequest().bodyValue(
                ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            ));
    }
    
    public Mono<ServerResponse> getBlockedUsers(ServerRequest request) {
        Long blockerId = (Long) request.attributes().get("userId");
        
        return securityService.getBlockedUsers(blockerId)
            .collectList()
            .flatMap(users -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                ApiResponse.<List<UserDto>>builder()
                    .success(true)
                    .data(users)
                    .build()
            ));
    }
    
    public Mono<ServerResponse> reportContent(ServerRequest request) {
        Long reporterId = (Long) request.attributes().get("userId");
        
        return request.bodyToMono(ReportDto.class)
            .flatMap(reportDto -> securityService.reportContent(reporterId, reportDto))
            .then(ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                ApiResponse.builder()
                    .success(true)
                    .message("신고가 접수되었습니다")
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