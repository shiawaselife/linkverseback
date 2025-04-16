package site.linkverse.back.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import site.linkverse.back.dto.BookmarkDto;
import site.linkverse.back.dto.CollectionDto;
import site.linkverse.back.model.Bookmark;
import site.linkverse.back.model.Collection;
import site.linkverse.back.repository.BookmarkRepository;
import site.linkverse.back.repository.CollectionRepository;
import site.linkverse.back.repository.PostRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BookmarkService {
    private final BookmarkRepository bookmarkRepository;
    private final CollectionRepository collectionRepository;
    private final PostRepository postRepository;
    
    public Mono<BookmarkDto> toggleBookmark(Long userId, Long postId, Long collectionId) {
        return bookmarkRepository.findByUserIdAndPostId(userId, postId)
            .flatMap(existingBookmark -> bookmarkRepository.delete(existingBookmark).thenReturn(false))
            .switchIfEmpty(Mono.defer(() -> {
                return postRepository.findById(postId)
                    .switchIfEmpty(Mono.error(new RuntimeException("게시물을 찾을 수 없습니다")))
                    .flatMap(post -> {
                        if (post.isDeleted()) {
                            return Mono.error(new RuntimeException("삭제된 게시물은 북마크할 수 없습니다"));
                        }
                        
                        Mono<Collection> collectionMono;
                        if (collectionId != null) {
                            collectionMono = collectionRepository.findById(collectionId)
                                .switchIfEmpty(Mono.error(new RuntimeException("컬렉션을 찾을 수 없습니다")));
                        } else {
                            collectionMono = Mono.just(null);
                        }
                        
                        return collectionMono.flatMap(collection -> {
                            Bookmark bookmark = Bookmark.builder()
                                .userId(userId)
                                .postId(postId)
                                .collectionId(collection != null ? collection.getId() : null)
                                .createdAt(LocalDateTime.now())
                                .build();
                                
                            return bookmarkRepository.save(bookmark)
                                .map(savedBookmark -> true);
                        });
                    });
            }))
            .flatMap(created -> {
                if (created) {
                    return bookmarkRepository.findByUserIdAndPostId(userId, postId)
                        .map(bookmark -> BookmarkDto.builder()
                            .id(bookmark.getId())
                            .userId(bookmark.getUserId())
                            .postId(bookmark.getPostId())
                            .collectionId(bookmark.getCollectionId())
                            .createdAt(bookmark.getCreatedAt())
                            .build());
                } else {
                    return Mono.empty();
                }
            });
    }
    
    public Flux<BookmarkDto> getUserBookmarks(Long userId, int page, int size) {
        return bookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
            .flatMap(bookmark -> postRepository.findById(bookmark.getPostId())
                .filter(post -> !post.isDeleted())
                .map(post -> BookmarkDto.builder()
                    .id(bookmark.getId())
                    .userId(bookmark.getUserId())
                    .postId(bookmark.getPostId())
                    .collectionId(bookmark.getCollectionId())
                    .createdAt(bookmark.getCreatedAt())
                    .build()));
    }
    
    public Mono<CollectionDto> createCollection(Long userId, String name) {
        Collection collection = Collection.builder()
            .userId(userId)
            .name(name)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
            
        return collectionRepository.save(collection)
            .map(savedCollection -> CollectionDto.builder()
                .id(savedCollection.getId())
                .userId(savedCollection.getUserId())
                .name(savedCollection.getName())
                .createdAt(savedCollection.getCreatedAt())
                .updatedAt(savedCollection.getUpdatedAt())
                .postsCount(0)
                .build());
    }
    
    public Flux<CollectionDto> getUserCollections(Long userId) {
        return collectionRepository.findByUserId(userId)
            .flatMap(collection -> bookmarkRepository.countByUserIdAndCollectionId(userId, collection.getId())
                .map(count -> CollectionDto.builder()
                    .id(collection.getId())
                    .userId(collection.getUserId())
                    .name(collection.getName())
                    .createdAt(collection.getCreatedAt())
                    .updatedAt(collection.getUpdatedAt())
                    .postsCount(count.intValue())
                    .build()));
    }
}