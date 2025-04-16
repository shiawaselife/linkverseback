package site.linkverse.back.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import site.linkverse.back.enums.LikeTargetType;
import site.linkverse.back.model.Like;

@Repository
public interface LikeRepository extends R2dbcRepository<Like, Long> {
    Mono<Like> findByUserIdAndTargetIdAndTargetType(Long userId, Long targetId, LikeTargetType targetType);
    Mono<Boolean> existsByUserIdAndTargetIdAndTargetType(Long userId, Long targetId, LikeTargetType targetType);
    Mono<Long> countByTargetIdAndTargetType(Long targetId, LikeTargetType targetType);
    Mono<Void> deleteByUserIdAndTargetIdAndTargetType(Long userId, Long targetId, LikeTargetType targetType);
    Flux<Like> findByTargetIdAndTargetTypeOrderByCreatedAtDesc(Long targetId, LikeTargetType targetType, Pageable pageable);
}