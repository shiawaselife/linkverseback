package site.linkverse.back.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import site.linkverse.back.model.Notification;

import java.util.List;

@Repository
public interface NotificationRepository extends R2dbcRepository<Notification, Long> {
    Flux<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Mono<Long> countByUserIdAndIsReadFalse(Long userId);
    @Query("UPDATE notifications SET is_read = true WHERE id IN (:ids)")
    Mono<Void> updateReadStatusByIds(List<Long> ids);
}