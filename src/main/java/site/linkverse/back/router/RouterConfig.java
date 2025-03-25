package site.linkverse.back.router;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import site.linkverse.back.handler.UserHandler;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
@RequiredArgsConstructor
public class RouterConfig {

    private final UserHandler userHandler;

    @Bean
    public RouterFunction<ServerResponse> apiRoutes() {
        return route()
                // 사용자 관련 라우트
                .nest(path("/api/auth"), builder -> builder
                        .POST("/register", accept(MediaType.APPLICATION_JSON), userHandler::register)
                        .POST("/login", accept(MediaType.APPLICATION_JSON), userHandler::login)
                        //.GET("/verify/{token}", userHandler::verifyEmail)
                )
                // 사용자 프로필 관련 라우트
                .nest(path("/api/users"), builder -> builder
                        .GET("/{id}", userHandler::getUserProfile)
                        .PUT("", accept(MediaType.APPLICATION_JSON), userHandler::updateProfile)
                )
                .build();
    }
}