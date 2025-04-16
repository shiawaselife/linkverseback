package site.linkverse.back.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import site.linkverse.back.model.Message;

import java.util.List;

@Repository
public interface MessageRepository extends R2dbcRepository<Message, Long> {
    @Query("SELECT * FROM messages WHERE " +
           "((sender_id = :userId AND receiver_id = :otherUserId) OR " +
           "(sender_id = :otherUserId AND receiver_id = :userId)) " +
           "ORDER BY created_at DESC")
    Flux<Message> findConversation(Long userId, Long otherUserId, Pageable pageable);
    
    Flux<Message> findByGroupIdOrderByCreatedAtDesc(Long groupId, Pageable pageable);
    
    @Query("SELECT DISTINCT " +
           "CASE WHEN sender_id = :userId THEN receiver_id ELSE sender_id END as other_user_id " +
           "FROM messages WHERE sender_id = :userId OR receiver_id = :userId " +
           "ORDER BY MAX(created_at) DESC")
    Flux<Long> findRecentConversationUserIds(Long userId, Pageable pageable);
    
    Mono<Long> countByReceiverIdAndIsReadFalse(Long receiverId);

    @Query("UPDATE messages SET is_read = true WHERE id IN (:ids)")
    Mono<Void> updateReadStatusByIds(List<Long> ids);
}