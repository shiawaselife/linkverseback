package site.linkverse.back.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import site.linkverse.back.model.Hashtag;

@Repository
public interface HashtagRepository extends R2dbcRepository<Hashtag, Long> {
    Mono<Hashtag> findByName(String name);
    
    @Query("SELECT h.* FROM hashtags h " +
           "JOIN post_hashtags ph ON h.id = ph.hashtag_id " +
           "GROUP BY h.id " +
           "ORDER BY COUNT(ph.id) DESC LIMIT :limit")
    Flux<Hashtag> findTrendingHashtags(int limit);
}