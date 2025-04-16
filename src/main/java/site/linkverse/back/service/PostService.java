package site.linkverse.back.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import site.linkverse.back.dto.*;
import site.linkverse.back.enums.LikeTargetType;
import site.linkverse.back.enums.MediaType;
import site.linkverse.back.model.Hashtag;
import site.linkverse.back.model.Media;
import site.linkverse.back.model.Post;
import site.linkverse.back.model.PostHashtag;
import site.linkverse.back.repository.*;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final MediaRepository mediaRepository;
    private final HashtagRepository hashtagRepository;
    private final PostHashtagRepository postHashtagRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final BookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;
    private final TransactionalOperator transactionalOperator;
    
    public Mono<PostDto> createPost(Long userId, PostCreateDto postCreateDto) {
        Post post = Post.builder()
            .userId(userId)
            .content(postCreateDto.getContent())
            .location(postCreateDto.getLocation())
            .visibility(postCreateDto.getVisibility())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .isDeleted(false)
            .build();
        
        return transactionalOperator.transactional(
            postRepository.save(post)
                .flatMap(savedPost -> {
                    // 미디어 저장
                    Flux<Media> mediaFlux = Flux.empty();
                    if (postCreateDto.getMediaUrls() != null && !postCreateDto.getMediaUrls().isEmpty()) {
                        mediaFlux = Flux.fromIterable(postCreateDto.getMediaUrls())
                            .map(url -> {
                                MediaType mediaType = determineMediaType(url);
                                return Media.builder()
                                    .postId(savedPost.getId())
                                    .mediaType(mediaType)
                                    .url(url)
                                    .createdAt(LocalDateTime.now())
                                    .build();
                            })
                            .flatMap(mediaRepository::save);
                    }
                    
                    // 해시태그 저장
                    Flux<PostHashtag> hashtagFlux = Flux.empty();
                    if (postCreateDto.getHashtags() != null && !postCreateDto.getHashtags().isEmpty()) {
                        hashtagFlux = Flux.fromIterable(postCreateDto.getHashtags())
                            .flatMap(tag -> hashtagRepository.findByName(tag)
                                .switchIfEmpty(hashtagRepository.save(Hashtag.builder()
                                    .name(tag)
                                    .build()))
                                .flatMap(hashtag -> postHashtagRepository.save(PostHashtag.builder()
                                    .postId(savedPost.getId())
                                    .hashtagId(hashtag.getId())
                                    .build())));
                    }
                    
                    return Flux.merge(mediaFlux, hashtagFlux)
                        .then(Mono.just(savedPost));
                })
                .flatMap(this::enrichPostWithDetails)
        );
    }
    
    public Mono<PostDto> getPostById(Long postId, Long currentUserId) {
        return postRepository.findById(postId)
            .switchIfEmpty(Mono.error(new RuntimeException("게시물을 찾을 수 없습니다")))
            .filter(post -> !post.isDeleted())
            .switchIfEmpty(Mono.error(new RuntimeException("게시물이 삭제되었습니다")))
            .flatMap(post -> enrichPostWithDetails(post, currentUserId));
    }
    
    public Flux<PostDto> getFeedPosts(Long userId, int page, int size) {
        return postRepository.findFeedPosts(userId, PageRequest.of(page, size))
            .filter(post -> !post.isDeleted())
            .flatMap(post -> enrichPostWithDetails(post, userId));
    }
    
    public Flux<PostDto> getUserPosts(Long userId, Long currentUserId, int page, int size) {
        return postRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
            .filter(post -> !post.isDeleted())
            .flatMap(post -> enrichPostWithDetails(post, currentUserId));
    }
    
    public Flux<PostDto> getPostsByHashtag(String hashtag, Long currentUserId, int page, int size) {
        return postRepository.findByHashtag(hashtag, PageRequest.of(page, size))
            .filter(post -> !post.isDeleted())
            .flatMap(post -> enrichPostWithDetails(post, currentUserId));
    }
    
    public Flux<PostDto> searchPosts(String keyword, Long currentUserId, int page, int size) {
        return postRepository.searchByKeyword("%" + keyword + "%", PageRequest.of(page, size))
            .filter(post -> !post.isDeleted())
            .flatMap(post -> enrichPostWithDetails(post, currentUserId));
    }
    
    public Mono<PostDto> updatePost(Long postId, Long userId, PostUpdateDto postUpdateDto) {
        return postRepository.findById(postId)
            .switchIfEmpty(Mono.error(new RuntimeException("게시물을 찾을 수 없습니다")))
            .filter(post -> post.getUserId().equals(userId))
            .switchIfEmpty(Mono.error(new RuntimeException("게시물을 수정할 권한이 없습니다")))
            .flatMap(post -> {
                post.setContent(postUpdateDto.getContent());
                post.setLocation(postUpdateDto.getLocation());
                post.setVisibility(postUpdateDto.getVisibility());
                post.setUpdatedAt(LocalDateTime.now());
                
                return transactionalOperator.transactional(
                    postRepository.save(post)
                        .flatMap(savedPost -> {
                            // 기존 미디어 및 해시태그 삭제
                            Mono<Void> deleteMedia = mediaRepository.deleteByPostId(savedPost.getId());
                            Mono<Void> deleteHashtags = postHashtagRepository.deleteByPostId(savedPost.getId());
                            
                            return Mono.when(deleteMedia, deleteHashtags)
                                .then(Mono.just(savedPost));
                        })
                        .flatMap(savedPost -> {
                            // 새 미디어 저장
                            Flux<Media> mediaFlux = Flux.empty();
                            if (postUpdateDto.getMediaUrls() != null && !postUpdateDto.getMediaUrls().isEmpty()) {
                                mediaFlux = Flux.fromIterable(postUpdateDto.getMediaUrls())
                                    .map(url -> {
                                        MediaType mediaType = determineMediaType(url);
                                        return Media.builder()
                                            .postId(savedPost.getId())
                                            .mediaType(mediaType)
                                            .url(url)
                                            .createdAt(LocalDateTime.now())
                                            .build();
                                    })
                                    .flatMap(mediaRepository::save);
                            }
                            
                            // 새 해시태그 저장
                            Flux<PostHashtag> hashtagFlux = Flux.empty();
                            if (postUpdateDto.getHashtags() != null && !postUpdateDto.getHashtags().isEmpty()) {
                                hashtagFlux = Flux.fromIterable(postUpdateDto.getHashtags())
                                    .flatMap(tag -> hashtagRepository.findByName(tag)
                                        .switchIfEmpty(hashtagRepository.save(Hashtag.builder()
                                            .name(tag)
                                            .build()))
                                        .flatMap(hashtag -> postHashtagRepository.save(PostHashtag.builder()
                                            .postId(savedPost.getId())
                                            .hashtagId(hashtag.getId())
                                            .build())));
                            }
                            
                            return Flux.merge(mediaFlux, hashtagFlux)
                                .then(Mono.just(savedPost));
                        })
                        .flatMap(savedPost -> enrichPostWithDetails(savedPost, userId))
                );
            });
    }
    
    public Mono<Void> deletePost(Long postId, Long userId) {
        return postRepository.findById(postId)
            .switchIfEmpty(Mono.error(new RuntimeException("게시물을 찾을 수 없습니다")))
            .filter(post -> post.getUserId().equals(userId))
            .switchIfEmpty(Mono.error(new RuntimeException("게시물을 삭제할 권한이 없습니다")))
            .flatMap(post -> {
                post.setDeleted(true);
                post.setUpdatedAt(LocalDateTime.now());
                return postRepository.save(post);
            })
            .then();
    }
    
    private Mono<PostDto> enrichPostWithDetails(Post post) {
        return enrichPostWithDetails(post, null);
    }
    
    private Mono<PostDto> enrichPostWithDetails(Post post, Long currentUserId) {
        PostDto.PostDtoBuilder postDtoBuilder = PostDto.builder()
            .id(post.getId())
            .userId(post.getUserId())
            .content(post.getContent())
            .location(post.getLocation())
            .visibility(post.getVisibility())
            .createdAt(post.getCreatedAt())
            .updatedAt(post.getUpdatedAt());
        
        Mono<UserDto> userMono = userRepository.findById(post.getUserId())
            .map(user -> UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .profileImage(user.getProfileImage())
                .build());
        
        Mono<List<MediaDto>> mediaMono = mediaRepository.findByPostId(post.getId())
            .map(media -> MediaDto.builder()
                .id(media.getId())
                .postId(media.getPostId())
                .mediaType(media.getMediaType())
                .url(media.getUrl())
                .createdAt(media.getCreatedAt())
                .build())
            .collectList();
        
        Mono<List<String>> hashtagsMono = postHashtagRepository.findByPostId(post.getId())
            .flatMap(postHashtag -> hashtagRepository.findById(postHashtag.getHashtagId()))
            .map(Hashtag::getName)
            .collectList();
        
        Mono<Integer> likeCountMono = likeRepository.countByTargetIdAndTargetType(post.getId(), LikeTargetType.POST)
            .map(Long::intValue);
        
        Mono<Integer> commentCountMono = commentRepository.countByPostId(post.getId())
            .map(Long::intValue);
        
        Mono<Boolean> isLikedMono = currentUserId != null 
            ? likeRepository.existsByUserIdAndTargetIdAndTargetType(currentUserId, post.getId(), LikeTargetType.POST)
            : Mono.just(false);
        
        Mono<Boolean> isBookmarkedMono = currentUserId != null
            ? bookmarkRepository.existsByUserIdAndPostId(currentUserId, post.getId())
            : Mono.just(false);
        
        return Mono.zip(userMono, mediaMono, hashtagsMono, likeCountMono, commentCountMono, isLikedMono, isBookmarkedMono)
            .map(tuple -> {
                UserDto user = tuple.getT1();
                List<MediaDto> media = tuple.getT2();
                List<String> hashtags = tuple.getT3();
                Integer likesCount = tuple.getT4();
                Integer commentsCount = tuple.getT5();
                Boolean isLiked = tuple.getT6();
                Boolean isBookmarked = tuple.getT7();
                
                return postDtoBuilder
                    .user(user)
                    .media(media)
                    .hashtags(hashtags)
                    .likesCount(likesCount)
                    .commentsCount(commentsCount)
                    .isLiked(isLiked)
                    .isBookmarked(isBookmarked)
                    .build();
            });
    }
    
    private MediaType determineMediaType(String url) {
        String lowerUrl = url.toLowerCase();
        if (lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg") || lowerUrl.endsWith(".png") || lowerUrl.endsWith(".gif")) {
            return MediaType.IMAGE;
        } else if (lowerUrl.endsWith(".mp4") || lowerUrl.endsWith(".avi") || lowerUrl.endsWith(".mov")) {
            return MediaType.VIDEO;
        } else if (lowerUrl.endsWith(".mp3") || lowerUrl.endsWith(".wav")) {
            return MediaType.AUDIO;
        } else {
            return MediaType.DOCUMENT;
        }
    }
}