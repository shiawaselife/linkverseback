package site.linkverse.back.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import site.linkverse.back.dto.FollowDto;
import site.linkverse.back.dto.UserDto;
import site.linkverse.back.model.Follow;
import site.linkverse.back.repository.FollowRepository;
import site.linkverse.back.repository.UserRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FollowService {
    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    
    public Mono<FollowDto> toggleFollow(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            return Mono.error(new RuntimeException("자기 자신을 팔로우할 수 없습니다"));
        }
        
        return userRepository.findById(followingId)
            .switchIfEmpty(Mono.error(new RuntimeException("사용자를 찾을 수 없습니다")))
            .flatMap(followingUser -> {
                return followRepository.findByFollowerIdAndFollowingId(followerId, followingId)
                    .flatMap(existingFollow -> followRepository.delete(existingFollow).thenReturn(false))
                    .switchIfEmpty(Mono.defer(() -> {
                        Follow follow = Follow.builder()
                            .followerId(followerId)
                            .followingId(followingId)
                            .createdAt(LocalDateTime.now())
                            .build();
                            
                        return followRepository.save(follow)
                            .map(savedFollow -> true);
                    }))
                    .flatMap(created -> {
                        if (created) {
                            return followRepository.findByFollowerIdAndFollowingId(followerId, followingId)
                                .flatMap(this::convertToDto);
                        } else {
                            return Mono.empty();
                        }
                    });
            });
    }
    
    public Flux<UserDto> getFollowers(Long userId, int page, int size) {
        return followRepository.findByFollowingId(userId, PageRequest.of(page, size))
            .flatMap(follow -> userRepository.findById(follow.getFollowerId()))
            .map(user -> UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .profileImage(user.getProfileImage())
                .bio(user.getBio())
                .profileVisibility(user.getProfileVisibility())
                .build());
    }
    
    public Flux<UserDto> getFollowing(Long userId, int page, int size) {
        return followRepository.findByFollowerId(userId, PageRequest.of(page, size))
            .flatMap(follow -> userRepository.findById(follow.getFollowingId()))
            .map(user -> UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .profileImage(user.getProfileImage())
                .bio(user.getBio())
                .profileVisibility(user.getProfileVisibility())
                .build());
    }
    
    private Mono<FollowDto> convertToDto(Follow follow) {
        FollowDto.FollowDtoBuilder followDtoBuilder = FollowDto.builder()
            .id(follow.getId())
            .followerId(follow.getFollowerId())
            .followingId(follow.getFollowingId())
            .createdAt(follow.getCreatedAt());
            
        Mono<UserDto> followerMono = userRepository.findById(follow.getFollowerId())
            .map(user -> UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .profileImage(user.getProfileImage())
                .build());
                
        Mono<UserDto> followingMono = userRepository.findById(follow.getFollowingId())
            .map(user -> UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .profileImage(user.getProfileImage())
                .build());
                
        return Mono.zip(followerMono, followingMono)
            .map(tuple -> {
                UserDto follower = tuple.getT1();
                UserDto following = tuple.getT2();
                
                return followDtoBuilder
                    .follower(follower)
                    .following(following)
                    .build();
            });
    }
}