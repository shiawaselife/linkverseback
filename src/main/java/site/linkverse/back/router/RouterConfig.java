package site.linkverse.back.router;

import site.linkverse.back.handler.*;
import site.linkverse.back.config.AuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;

@Configuration
public class RouterConfig {

    @Bean
    public RouterFunction<ServerResponse> routes(
            UserHandler userHandler,
            PostHandler postHandler,
            CommentHandler commentHandler,
            LikeHandler likeHandler,
            BookmarkHandler bookmarkHandler,
            FollowHandler followHandler,
            MessageHandler messageHandler,
            NotificationHandler notificationHandler,
            SearchHandler searchHandler,
            SecurityHandler securityHandler,
            AuthenticationFilter authFilter) {

        // 인증이 필요 없는 공개 라우트
        RouterFunction<ServerResponse> publicRoutes = RouterFunctions
                .route(POST("/api/auth/register"), userHandler::register)
                .andRoute(POST("/api/auth/login"), userHandler::login)
                .andRoute(GET("/api/users/{id}").and(accept(MediaType.APPLICATION_JSON)), userHandler::getUserInfo)
                .andRoute(GET("/api/users/search").and(accept(MediaType.APPLICATION_JSON)), userHandler::searchUsers)
                .andRoute(GET("/api/likes/{targetType}/{targetId}").and(accept(MediaType.APPLICATION_JSON)), likeHandler::getLikes)
                .andRoute(GET("/api/users/{userId}/followers").and(accept(MediaType.APPLICATION_JSON)), followHandler::getFollowers)
                .andRoute(GET("/api/users/{userId}/following").and(accept(MediaType.APPLICATION_JSON)), followHandler::getFollowing)
                .andRoute(GET("/api/search/users").and(accept(MediaType.APPLICATION_JSON)), searchHandler::searchUsers)
                .andRoute(GET("/api/search/hashtags").and(accept(MediaType.APPLICATION_JSON)), searchHandler::searchHashtags)
                .andRoute(GET("/api/hashtags/trending").and(accept(MediaType.APPLICATION_JSON)), searchHandler::getTrendingHashtags);

        // 인증이 필요한 보호된 라우트
        RouterFunction<ServerResponse> protectedRoutes = RouterFunctions
                .route(PUT("/api/users/{id}").and(accept(MediaType.APPLICATION_JSON)), userHandler::updateUser)
                .andRoute(PUT("/api/users/{id}/password").and(accept(MediaType.APPLICATION_JSON)), userHandler::updatePassword)
                .andRoute(POST("/api/posts").and(accept(MediaType.APPLICATION_JSON)), postHandler::createPost)
                .andRoute(GET("/api/posts/{id}").and(accept(MediaType.APPLICATION_JSON)), postHandler::getPost)
                .andRoute(GET("/api/feed").and(accept(MediaType.APPLICATION_JSON)), postHandler::getFeedPosts)
                .andRoute(GET("/api/users/{userId}/posts").and(accept(MediaType.APPLICATION_JSON)), postHandler::getUserPosts)
                .andRoute(GET("/api/hashtags/{hashtag}/posts").and(accept(MediaType.APPLICATION_JSON)), postHandler::getHashtagPosts)
                .andRoute(PUT("/api/posts/{id}").and(accept(MediaType.APPLICATION_JSON)), postHandler::updatePost)
                .andRoute(DELETE("/api/posts/{id}").and(accept(MediaType.APPLICATION_JSON)), postHandler::deletePost)
                .andRoute(GET("/api/posts/search").and(accept(MediaType.APPLICATION_JSON)), postHandler::searchPosts)
                .andRoute(POST("/api/comments").and(accept(MediaType.APPLICATION_JSON)), commentHandler::createComment)
                .andRoute(GET("/api/posts/{postId}/comments").and(accept(MediaType.APPLICATION_JSON)), commentHandler::getPostComments)
                .andRoute(PUT("/api/comments/{id}").and(accept(MediaType.APPLICATION_JSON)), commentHandler::updateComment)
                .andRoute(DELETE("/api/comments/{id}").and(accept(MediaType.APPLICATION_JSON)), commentHandler::deleteComment)
                .andRoute(POST("/api/likes/{targetType}/{targetId}").and(accept(MediaType.APPLICATION_JSON)), likeHandler::toggleLike)
                .andRoute(POST("/api/bookmarks/{postId}").and(accept(MediaType.APPLICATION_JSON)), bookmarkHandler::toggleBookmark)
                .andRoute(GET("/api/bookmarks").and(accept(MediaType.APPLICATION_JSON)), bookmarkHandler::getUserBookmarks)
                .andRoute(POST("/api/collections").and(accept(MediaType.APPLICATION_JSON)), bookmarkHandler::createCollection)
                .andRoute(GET("/api/collections").and(accept(MediaType.APPLICATION_JSON)), bookmarkHandler::getUserCollections)
                .andRoute(POST("/api/follow/{followingId}").and(accept(MediaType.APPLICATION_JSON)), followHandler::toggleFollow)
                .andRoute(POST("/api/messages").and(accept(MediaType.APPLICATION_JSON)), messageHandler::sendMessage)
                .andRoute(GET("/api/messages/users/{otherUserId}").and(accept(MediaType.APPLICATION_JSON)), messageHandler::getConversation)
                .andRoute(GET("/api/messages/groups/{groupId}").and(accept(MediaType.APPLICATION_JSON)), messageHandler::getGroupMessages)
                .andRoute(GET("/api/messages/recent").and(accept(MediaType.APPLICATION_JSON)), messageHandler::getRecentConversations)
                .andRoute(PUT("/api/messages/read").and(accept(MediaType.APPLICATION_JSON)), messageHandler::markAsRead)
                .andRoute(GET("/api/notifications").and(accept(MediaType.APPLICATION_JSON)), notificationHandler::getUserNotifications)
                .andRoute(GET("/api/notifications/unread/count").and(accept(MediaType.APPLICATION_JSON)), notificationHandler::countUnreadNotifications)
                .andRoute(PUT("/api/notifications/read").and(accept(MediaType.APPLICATION_JSON)), notificationHandler::markAsRead)
                .andRoute(GET("/api/search/posts").and(accept(MediaType.APPLICATION_JSON)), searchHandler::searchPosts)
                .andRoute(POST("/api/security/block").and(accept(MediaType.APPLICATION_JSON)), securityHandler::blockUser)
                .andRoute(DELETE("/api/security/block/{blockedId}").and(accept(MediaType.APPLICATION_JSON)), securityHandler::unblockUser)
                .andRoute(GET("/api/security/blocked").and(accept(MediaType.APPLICATION_JSON)), securityHandler::getBlockedUsers)
                .andRoute(POST("/api/security/report").and(accept(MediaType.APPLICATION_JSON)), securityHandler::reportContent);

        // 보호된 라우트에 인증 필터를 적용하고 공개 라우트와 결합
        return publicRoutes.and(protectedRoutes.filter(authFilter));
    }
}