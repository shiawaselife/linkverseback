package site.linkverse.back.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import site.linkverse.back.model.Media;

@Repository
public interface MediaRepository extends R2dbcRepository<Media, Long> {
    Flux<Media> findByPostId(Long postId);
    Mono<Void> deleteByPostId(Long postId);
}