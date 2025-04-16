package site.linkverse.back.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import site.linkverse.back.model.PostHashtag;

@Repository
public interface PostHashtagRepository extends R2dbcRepository<PostHashtag, Long> {
    Flux<PostHashtag> findByPostId(Long postId);
    Mono<Void> deleteByPostId(Long postId);
}