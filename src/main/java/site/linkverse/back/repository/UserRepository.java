package site.linkverse.back.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import site.linkverse.back.model.User;

@Repository
public interface UserRepository extends ReactiveCrudRepository<User, Long> {

    // 기본 CRUD 작업은 ReactiveCrudRepository에서 상속

    // 이메일로 사용자 찾기
    Mono<User> findByEmail(String email);

    // 이메일 존재 여부 확인
    Mono<Boolean> existsByEmail(String email);

    // 사용자명 존재 여부 확인
    Mono<Boolean> existsByUsername(String username);

    // 사용자명으로 사용자 찾기
    Mono<User> findByUsername(String username);

    // 이메일 인증 처리
    @Query("UPDATE users SET email_verified = true WHERE email = :email")
    Mono<Void> verifyEmail(String email);

    // 사용자명으로 검색
    Flux<User> findByUsernameContaining(String username);

    // 팔로우 중인 사용자 목록 조회
    @Query("SELECT u.* FROM users u " +
            "JOIN follows f ON u.id = f.following_id " +
            "WHERE f.follower_id = :userId")
    Flux<User> findFollowing(Long userId);

    // 팔로워 목록 조회
    @Query("SELECT u.* FROM users u " +
            "JOIN follows f ON u.id = f.follower_id " +
            "WHERE f.following_id = :userId")
    Flux<User> findFollowers(Long userId);

    // 팔로우 관계 확인
    @Query("SELECT COUNT(*) > 0 FROM follows " +
            "WHERE follower_id = :followerId AND following_id = :followingId")
    Mono<Boolean> isFollowing(Long followerId, Long followingId);

    // 팔로우 추가
    @Query("INSERT INTO follows (follower_id, following_id, created_at) " +
            "VALUES (:followerId, :followingId, NOW())")
    Mono<Void> follow(Long followerId, Long followingId);

    // 언팔로우
    @Query("DELETE FROM follows " +
            "WHERE follower_id = :followerId AND following_id = :followingId")
    Mono<Void> unfollow(Long followerId, Long followingId);

    // 추천 사용자 목록 조회 (팔로우하지 않은 사용자 중 인기 있는 사용자)
    @Query("SELECT u.* FROM users u " +
            "LEFT JOIN (SELECT following_id, COUNT(*) as followers_count FROM follows GROUP BY following_id) fc " +
            "ON u.id = fc.following_id " +
            "WHERE u.id != :userId " +
            "AND u.id NOT IN (SELECT following_id FROM follows WHERE follower_id = :userId) " +
            "AND u.id NOT IN (SELECT blocked_id FROM blocks WHERE blocker_id = :userId) " +
            "ORDER BY fc.followers_count DESC NULLS LAST " +
            "LIMIT :limit")
    Flux<User> findRecommendedUsers(Long userId, int limit);

    // 차단 관계 확인
    @Query("SELECT COUNT(*) > 0 FROM blocks " +
            "WHERE blocker_id = :blockerId AND blocked_id = :blockedId")
    Mono<Boolean> isBlocked(Long blockerId, Long blockedId);

    // 사용자 차단
    @Query("INSERT INTO blocks (blocker_id, blocked_id, created_at) " +
            "VALUES (:blockerId, :blockedId, NOW())")
    Mono<Void> blockUser(Long blockerId, Long blockedId);

    // 차단 해제
    @Query("DELETE FROM blocks " +
            "WHERE blocker_id = :blockerId AND blocked_id = :blockedId")
    Mono<Void> unblockUser(Long blockerId, Long blockedId);

    // 차단한 사용자 목록 조회
    @Query("SELECT u.* FROM users u " +
            "JOIN blocks b ON u.id = b.blocked_id " +
            "WHERE b.blocker_id = :userId")
    Flux<User> findBlockedUsers(Long userId);
}