package site.linkverse.back.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import site.linkverse.back.model.Bookmark;

@Repository
public interface BookmarkRepository extends R2dbcRepository<Bookmark, Long> {
    Mono<Bookmark> findByUserIdAndPostId(Long userId, Long postId);
    Mono<Boolean> existsByUserIdAndPostId(Long userId, Long postId);
    Flux<Bookmark> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Flux<Bookmark> findByUserIdAndCollectionIdOrderByCreatedAtDesc(Long userId, Long collectionId, Pageable pageable);
    Mono<Void> deleteByUserIdAndPostId(Long userId, Long postId);
    Mono<Long> countByUserIdAndCollectionId(Long userId, Long collectionId);

}