package site.linkverse.back.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import site.linkverse.back.dto.LikeDto;
import site.linkverse.back.dto.UserDto;
import site.linkverse.back.enums.LikeTargetType;
import site.linkverse.back.model.Like;
import site.linkverse.back.repository.CommentRepository;
import site.linkverse.back.repository.LikeRepository;
import site.linkverse.back.repository.PostRepository;
import site.linkverse.back.repository.UserRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LikeService {
    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    
    public Mono<LikeDto> toggleLike(Long userId, Long targetId, LikeTargetType targetType) {
        return likeRepository.findByUserIdAndTargetIdAndTargetType(userId, targetId, targetType)
            .flatMap(existingLike -> likeRepository.delete(existingLike).thenReturn(false))
            .switchIfEmpty(Mono.defer(() -> {
                if (targetType == LikeTargetType.POST) {
                    return postRepository.findById(targetId)
                        .switchIfEmpty(Mono.error(new RuntimeException("게시물을 찾을 수 없습니다")))
                        .flatMap(post -> {
                            if (post.isDeleted()) {
                                return Mono.error(new RuntimeException("삭제된 게시물에는 좋아요를 할 수 없습니다"));
                            }
                            return createLike(userId, targetId, targetType);
                        });
                } else {
                    return commentRepository.findById(targetId)
                        .switchIfEmpty(Mono.error(new RuntimeException("댓글을 찾을 수 없습니다")))
                        .flatMap(comment -> {
                            if (comment.isDeleted()) {
                                return Mono.error(new RuntimeException("삭제된 댓글에는 좋아요를 할 수 없습니다"));
                            }
                            return createLike(userId, targetId, targetType);
                        });
                }
            }))
            .flatMap(created -> {
                if (created) {
                    return likeRepository.findByUserIdAndTargetIdAndTargetType(userId, targetId, targetType)
                        .flatMap(this::convertToDto);
                } else {
                    return Mono.empty();
                }
            });
    }
    
    public Flux<LikeDto> getLikes(Long targetId, LikeTargetType targetType, int page, int size) {
        return likeRepository.findByTargetIdAndTargetTypeOrderByCreatedAtDesc(targetId, targetType, PageRequest.of(page, size))
            .flatMap(this::convertToDto);
    }
    
    private Mono<Boolean> createLike(Long userId, Long targetId, LikeTargetType targetType) {
        Like like = Like.builder()
            .userId(userId)
            .targetId(targetId)
            .targetType(targetType)
            .createdAt(LocalDateTime.now())
            .build();
        
        return likeRepository.save(like)
            .map(savedLike -> true);
    }
    
    private Mono<LikeDto> convertToDto(Like like) {
        LikeDto.LikeDtoBuilder likeDtoBuilder = LikeDto.builder()
            .id(like.getId())
            .userId(like.getUserId())
            .targetId(like.getTargetId())
            .targetType(like.getTargetType())
            .createdAt(like.getCreatedAt());
            
        return userRepository.findById(like.getUserId())
            .map(user -> UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .profileImage(user.getProfileImage())
                .build())
            .map(userDto -> likeDtoBuilder
                .user(userDto)
                .build());
    }
}