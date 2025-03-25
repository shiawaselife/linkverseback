package site.linkverse.back.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import site.linkverse.back.model.Comment;

public interface CommentRepository extends ReactiveCrudRepository<Comment, Long> {
    
    Flux<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);
    
    Flux<Comment> findByParentId(Long parentId);
}