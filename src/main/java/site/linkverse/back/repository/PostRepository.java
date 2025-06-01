package site.linkverse.back.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import site.linkverse.back.enums.VisibilityType;
import site.linkverse.back.model.Post;

import java.util.List;

@Repository
public interface PostRepository extends R2dbcRepository<Post, Long> {
    Flux<Post> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // 팔로잉 사용자 + 본인 게시글 (Following 탭용)
    @Query("SELECT p.* FROM posts p " +
            "LEFT JOIN follows f ON p.user_id = f.following_id " +
            "WHERE f.follower_id = :userId OR p.user_id = :userId " +
            "ORDER BY p.created_at DESC")
    Flux<Post> findFeedPosts(Long userId, Pageable pageable);

    // 모든 공개 게시글 (Recommend 탭용)
    Flux<Post> findAllByVisibilityAndIsDeletedOrderByCreatedAtDesc(
            VisibilityType visibility, boolean isDeleted, Pageable pageable);

    @Query("SELECT p.* FROM posts p " +
            "JOIN post_hashtags ph ON p.id = ph.post_id " +
            "JOIN hashtags h ON ph.hashtag_id = h.id " +
            "WHERE h.name = :hashtag " +
            "ORDER BY p.created_at DESC")
    Flux<Post> findByHashtag(String hashtag, Pageable pageable);

    @Query("SELECT * FROM posts WHERE content LIKE :keyword ORDER BY created_at DESC")
    Flux<Post> searchByKeyword(String keyword, Pageable pageable);
}