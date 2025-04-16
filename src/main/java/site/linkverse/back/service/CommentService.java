package site.linkverse.back.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import site.linkverse.back.dto.CommentCreateDto;
import site.linkverse.back.dto.CommentDto;
import site.linkverse.back.dto.CommentUpdateDto;
import site.linkverse.back.dto.UserDto;
import site.linkverse.back.enums.LikeTargetType;
import site.linkverse.back.model.Comment;
import site.linkverse.back.repository.CommentRepository;
import site.linkverse.back.repository.LikeRepository;
import site.linkverse.back.repository.PostRepository;
import site.linkverse.back.repository.UserRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final LikeRepository likeRepository;
    
    public Mono<CommentDto> createComment(Long userId, CommentCreateDto commentCreateDto) {
        return postRepository.findById(commentCreateDto.getPostId())
            .switchIfEmpty(Mono.error(new RuntimeException("게시물을 찾을 수 없습니다")))
            .flatMap(post -> {
                Comment comment = Comment.builder()
                    .postId(commentCreateDto.getPostId())
                    .userId(userId)
                    .parentId(commentCreateDto.getParentId())
                    .content(commentCreateDto.getContent())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .isDeleted(false)
                    .build();
                
                return commentRepository.save(comment)
                    .flatMap(savedComment -> enrichCommentWithDetails(savedComment, userId));
            });
    }
    
    public Flux<CommentDto> getPostComments(Long postId, Long currentUserId, int page, int size) {
        return commentRepository.findByPostIdAndParentIdIsNullOrderByCreatedAtAsc(postId, PageRequest.of(page, size))
            .filter(comment -> !comment.isDeleted())
            .flatMap(comment -> enrichCommentWithDetails(comment, currentUserId)
                .flatMap(commentDto -> commentRepository.findByParentIdOrderByCreatedAtAsc(comment.getId())
                    .filter(reply -> !reply.isDeleted())
                    .flatMap(reply -> enrichCommentWithDetails(reply, currentUserId))
                    .collectList()
                    .map(replies -> {
                        commentDto.setReplies(replies);
                        return commentDto;
                    })));
    }
    
    public Mono<CommentDto> updateComment(Long commentId, Long userId, CommentUpdateDto commentUpdateDto) {
        return commentRepository.findById(commentId)
            .switchIfEmpty(Mono.error(new RuntimeException("댓글을 찾을 수 없습니다")))
            .filter(comment -> comment.getUserId().equals(userId))
            .switchIfEmpty(Mono.error(new RuntimeException("댓글을 수정할 권한이 없습니다")))
            .flatMap(comment -> {
                comment.setContent(commentUpdateDto.getContent());
                comment.setUpdatedAt(LocalDateTime.now());
                
                return commentRepository.save(comment)
                    .flatMap(updatedComment -> enrichCommentWithDetails(updatedComment, userId));
            });
    }
    
    public Mono<Void> deleteComment(Long commentId, Long userId) {
        return commentRepository.findById(commentId)
            .switchIfEmpty(Mono.error(new RuntimeException("댓글을 찾을 수 없습니다")))
            .filter(comment -> comment.getUserId().equals(userId))
            .switchIfEmpty(Mono.error(new RuntimeException("댓글을 삭제할 권한이 없습니다")))
            .flatMap(comment -> {
                comment.setDeleted(true);
                comment.setUpdatedAt(LocalDateTime.now());
                return commentRepository.save(comment);
            })
            .then();
    }
    
    private Mono<CommentDto> enrichCommentWithDetails(Comment comment, Long currentUserId) {
        CommentDto.CommentDtoBuilder commentDtoBuilder = CommentDto.builder()
            .id(comment.getId())
            .postId(comment.getPostId())
            .userId(comment.getUserId())
            .parentId(comment.getParentId())
            .content(comment.getContent())
            .createdAt(comment.getCreatedAt())
            .updatedAt(comment.getUpdatedAt());
            
        Mono<UserDto> userMono = userRepository.findById(comment.getUserId())
            .map(user -> UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .profileImage(user.getProfileImage())
                .build());
                
        Mono<Integer> likeCountMono = likeRepository.countByTargetIdAndTargetType(comment.getId(), LikeTargetType.COMMENT)
            .map(Long::intValue);
                
        Mono<Integer> repliesCountMono = comment.getParentId() == null 
            ? commentRepository.countByParentId(comment.getId()).map(Long::intValue) 
            : Mono.just(0);
                
        Mono<Boolean> isLikedMono = currentUserId != null
            ? likeRepository.existsByUserIdAndTargetIdAndTargetType(currentUserId, comment.getId(), LikeTargetType.COMMENT)
            : Mono.just(false);
                
        return Mono.zip(userMono, likeCountMono, repliesCountMono, isLikedMono)
            .map(tuple -> {
                UserDto user = tuple.getT1();
                Integer likesCount = tuple.getT2();
                Integer repliesCount = tuple.getT3();
                Boolean isLiked = tuple.getT4();
                
                return commentDtoBuilder
                    .user(user)
                    .likesCount(likesCount)
                    .repliesCount(repliesCount)
                    .isLiked(isLiked)
                    .build();
            });
    }
}