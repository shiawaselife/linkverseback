package site.linkverse.back.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import site.linkverse.back.model.UserBlock;

@Repository
public interface UserBlockRepository extends R2dbcRepository<UserBlock, Long> {
    Mono<Boolean> existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
    Flux<UserBlock> findByBlockerId(Long blockerId);
    Mono<Void> deleteByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
}