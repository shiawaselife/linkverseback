package site.linkverse.back.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import site.linkverse.back.model.Collection;

@Repository
public interface CollectionRepository extends R2dbcRepository<Collection, Long> {
    Flux<Collection> findByUserId(Long userId);
    Mono<Long> countByUserIdAndIdIsNot(Long userId, Long collectionId);
}