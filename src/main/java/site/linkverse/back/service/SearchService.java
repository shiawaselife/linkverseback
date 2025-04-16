package site.linkverse.back.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import site.linkverse.back.dto.MediaDto;
import site.linkverse.back.dto.PostDto;
import site.linkverse.back.dto.UserDto;
import site.linkverse.back.enums.LikeTargetType;
import site.linkverse.back.model.Hashtag;
import site.linkverse.back.repository.*;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final HashtagRepository hashtagRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final BookmarkRepository bookmarkRepository;
    private final MediaRepository mediaRepository;
    
    public Flux<UserDto> searchUsers(String keyword, int page, int size) {
        return userRepository.searchByKeyword("%" + keyword + "%", PageRequest.of(page, size))
            .map(user -> UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .profileImage(user.getProfileImage())
                .bio(user.getBio())
                .profileVisibility(user.getProfileVisibility())
                .build());
    }
    
    public Flux<PostDto> searchPosts(String keyword, Long currentUserId, int page, int size) {
        return postRepository.searchByKeyword("%" + keyword + "%", PageRequest.of(page, size))
            .filter(post -> !post.isDeleted())
            .flatMap(post -> {
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
                
                return Mono.zip(userMono, mediaMono, likeCountMono, commentCountMono, isLikedMono, isBookmarkedMono)
                    .map(tuple -> {
                        UserDto user = tuple.getT1();
                        List<MediaDto> media = tuple.getT2();
                        Integer likesCount = tuple.getT3();
                        Integer commentsCount = tuple.getT4();
                        Boolean isLiked = tuple.getT5();
                        Boolean isBookmarked = tuple.getT6();
                        
                        return postDtoBuilder
                            .user(user)
                            .media(media)
                            .likesCount(likesCount)
                            .commentsCount(commentsCount)
                            .isLiked(isLiked)
                            .isBookmarked(isBookmarked)
                            .build();
                    });
            });
    }
    
    public Flux<String> searchHashtags(String keyword, int limit) {
        // 실제 구현에서는 해시태그 테이블을 직접 쿼리하는 방식이 필요
        return hashtagRepository.findAll()
            .filter(hashtag -> hashtag.getName().contains(keyword))
            .map(Hashtag::getName)
            .take(limit);
    }
    
    public Flux<String> getTrendingHashtags(int limit) {
        return hashtagRepository.findTrendingHashtags(limit)
            .map(Hashtag::getName);
    }
}