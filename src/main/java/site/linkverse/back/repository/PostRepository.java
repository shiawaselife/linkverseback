package site.linkverse.back.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import site.linkverse.back.model.Post;

public interface PostRepository extends ReactiveCrudRepository<Post, Long> {
    
    Flux<Post> findByUserId(Long userId);
    
    @Query("SELECT p.* FROM posts p " +
           "WHERE p.visibility = 'PUBLIC' OR " +
           "(p.visibility = 'FRIENDS' AND p.user_id IN " +
           "(SELECT following_id FROM follows WHERE follower_id = :userId)) " +
           "ORDER BY p.created_at DESC LIMIT :limit OFFSET :offset")
    Flux<Post> findFeedPosts(Long userId, int limit, int offset);
    
    @Query("SELECT p.* FROM posts p " +
           "JOIN post_hashtags ph ON p.id = ph.post_id " +
           "JOIN hashtags h ON ph.hashtag_id = h.id " +
           "WHERE h.name = :hashtag AND (p.visibility = 'PUBLIC' OR " +
           "(p.visibility = 'FRIENDS' AND p.user_id IN " +
           "(SELECT following_id FROM follows WHERE follower_id = :userId)))")
    Flux<Post> findByHashtag(String hashtag, Long userId);
}