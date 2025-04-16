package site.linkverse.back.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import site.linkverse.back.model.Follow;

@Repository
public interface FollowRepository extends R2dbcRepository<Follow, Long> {
    Mono<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);
    Mono<Boolean> existsByFollowerIdAndFollowingId(Long followerId, Long followingId);
    Flux<Follow> findByFollowerId(Long followerId, Pageable pageable);
    Flux<Follow> findByFollowingId(Long followingId, Pageable pageable);
    Mono<Long> countByFollowerId(Long followerId);
    Mono<Long> countByFollowingId(Long followingId);
    Mono<Void> deleteByFollowerIdAndFollowingId(Long followerId, Long followingId);
}