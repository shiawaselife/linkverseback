package site.linkverse.back.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import site.linkverse.back.model.Comment;

@Repository
public interface CommentRepository extends R2dbcRepository<Comment, Long> {
  Flux<Comment> findByPostIdAndParentIdIsNullOrderByCreatedAtAsc(Long postId, Pageable pageable);
  Flux<Comment> findByParentIdOrderByCreatedAtAsc(Long parentId);
  Mono<Long> countByPostId(Long postId);
  Mono<Long> countByParentId(Long parentId);
}