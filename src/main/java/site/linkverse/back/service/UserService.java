package site.linkverse.back.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import site.linkverse.back.dto.UserDto;
import site.linkverse.back.model.User;
import site.linkverse.back.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    /**
     * 새 사용자 등록 (회원가입)
     */
    public Mono<User> registerUser(UserDto userDto) {
        log.debug("사용자 등록 시작: {}", userDto.getEmail());
        
        // 이메일 중복 체크
        return userRepository.existsByEmail(userDto.getEmail())
                .flatMap(exists -> {
                    if (exists) {
                        log.warn("사용자 등록 실패: 이메일 이미 존재함 - {}", userDto.getEmail());
                        return Mono.error(new RuntimeException("이미 등록된 이메일입니다."));
                    }
                    
                    // 사용자명 중복 체크
                    return userRepository.existsByUsername(userDto.getUsername());
                })
                .flatMap(exists -> {
                    if (exists) {
                        log.warn("사용자 등록 실패: 사용자명 이미 존재함 - {}", userDto.getUsername());
                        return Mono.error(new RuntimeException("이미 사용 중인 사용자명입니다."));
                    }
                    
                    // 새 사용자 생성
                    User user = User.builder()
                            .email(userDto.getEmail())
                            .password(userDto.getPassword())
                            .username(userDto.getUsername())
                            .profileName(userDto.getProfileName())
                            .profileImage(userDto.getProfileImage())
                            .bio(userDto.getBio())
                            .profileVisibility(userDto.getProfileVisibility() != null && !userDto.getProfileVisibility().isBlank() ?
                                    userDto.getProfileVisibility() : "PUBLIC")
                            .emailVerified(false)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    
                    return userRepository.save(user);
                })
                .doOnSuccess(user -> log.info("사용자 등록 성공: {}", user.getEmail()));
    }

    /**
     * 이메일로 사용자 찾기
     */
    public Mono<User> findByEmail(String email) {
        return userRepository.findByEmail(email)
                .doOnSuccess(user -> {
                    if (user != null) {
                        log.debug("사용자 조회 성공 (이메일): {}", email);
                    } else {
                        log.debug("사용자 조회 결과 없음 (이메일): {}", email);
                    }
                });
    }

    /**
     * 사용자명으로 사용자 찾기
     */

    public Mono<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * ID로 사용자 찾기
     */
    public Mono<User> findById(Long id) {
        return userRepository.findById(id)
                .doOnSuccess(user -> {
                    if (user != null) {
                        log.debug("사용자 조회 성공 (ID): {}", id);
                    } else {
                        log.debug("사용자 조회 결과 없음 (ID): {}", id);
                    }
                });
    }

    /**
     * 사용자 정보 업데이트
     */
    public Mono<User> updateUser(Long id, UserDto userDto) {
        log.debug("사용자 정보 업데이트 시작: ID={}", id);
        
        return userRepository.findById(id)
                .flatMap(user -> {
                    // 사용자명 변경 시 중복 체크
                    if (userDto.getUsername() != null && !userDto.getUsername().equals(user.getUsername())) {
                        return userRepository.existsByUsername(userDto.getUsername())
                                .flatMap(exists -> {
                                    if (exists) {
                                        return Mono.error(new RuntimeException("이미 사용 중인 사용자명입니다."));
                                    }
                                    return updateUserFields(user, userDto);
                                });
                    }
                    
                    return updateUserFields(user, userDto);
                })
                .doOnSuccess(user -> log.info("사용자 정보 업데이트 성공: ID={}", id));
    }
    
    /**
     * 사용자 필드 업데이트 헬퍼 메서드
     */
    private Mono<User> updateUserFields(User user, UserDto userDto) {
        if (userDto.getUsername() != null) {
            user.setUsername(userDto.getUsername());
        }
        if (userDto.getProfileName() != null) {
            user.setUsername(userDto.getProfileName());
        }
        if (userDto.getProfileImage() != null) {
            user.setProfileImage(userDto.getProfileImage());
        }
        if (userDto.getBio() != null) {
            user.setBio(userDto.getBio());
        }
        if (userDto.getProfileVisibility() != null) {
            user.setProfileVisibility(userDto.getProfileVisibility());
        }
        if (userDto.getPassword() != null) {
            user.setPassword(userDto.getPassword());
        }
        
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    /**
     * 이메일 인증 완료 처리
     */
    public Mono<Void> verifyEmail(String email) {
        log.debug("이메일 인증 처리: {}", email);
        return userRepository.verifyEmail(email)
                .doOnSuccess(result -> log.info("이메일 인증 완료: {}", email));
    }

    /**
     * 사용자 검색 (이름으로)
     */

    public Flux<User> searchByUsername(String username) {
        log.debug("사용자 검색 시작: username={}", username);
        return userRepository.findByUsernameContaining(username)
                .doOnComplete(() -> log.debug("사용자 검색 완료: username={}", username));
    }

    /**
     * 팔로우 중인 사용자 목록 조회
     */
    public Flux<User> getFollowing(Long userId) {
        log.debug("팔로우 목록 조회: userId={}", userId);
        return userRepository.findFollowing(userId)
                .doOnComplete(() -> log.debug("팔로우 목록 조회 완료: userId={}", userId));
    }

    /**
     * 팔로워 목록 조회
     */
    public Flux<User> getFollowers(Long userId) {
        log.debug("팔로워 목록 조회: userId={}", userId);
        return userRepository.findFollowers(userId)
                .doOnComplete(() -> log.debug("팔로워 목록 조회 완료: userId={}", userId));
    }

    /**
     * 팔로우 하기
     */
    public Mono<Void> followUser(Long followerId, Long followingId) {
        log.debug("팔로우 시작: followerId={}, followingId={}", followerId, followingId);
        
        // 자기 자신은 팔로우할 수 없음
        if (followerId.equals(followingId)) {
            return Mono.error(new RuntimeException("자신을 팔로우할 수 없습니다."));
        }
        
        // 이미 팔로우 중인지 확인
        return userRepository.isFollowing(followerId, followingId)
                .flatMap(isFollowing -> {
                    if (isFollowing) {
                        return Mono.empty(); // 이미 팔로우 중이면 아무 작업도 하지 않음
                    }
                    
                    // 사용자 존재 확인
                    return userRepository.existsById(followingId)
                            .flatMap(exists -> {
                                if (!exists) {
                                    return Mono.error(new RuntimeException("팔로우할 사용자가 존재하지 않습니다."));
                                }
                                
                                return userRepository.follow(followerId, followingId);
                            });
                })
                .doOnSuccess(result -> log.info("팔로우 성공: followerId={}, followingId={}", followerId, followingId));
    }

    /**
     * 언팔로우 하기
     */
    public Mono<Void> unfollowUser(Long followerId, Long followingId) {
        log.debug("언팔로우 시작: followerId={}, followingId={}", followerId, followingId);
        
        return userRepository.unfollow(followerId, followingId)
                .doOnSuccess(result -> log.info("언팔로우 성공: followerId={}, followingId={}", followerId, followingId));
    }

    /**
     * 추천 사용자 목록 조회 (팔로우하지 않은 사용자 중 인기 있는 사용자)
     */
    public Flux<User> getRecommendedUsers(Long userId, int limit) {
        log.debug("추천 사용자 목록 조회: userId={}, limit={}", userId, limit);
        
        return userRepository.findRecommendedUsers(userId, limit)
                .doOnComplete(() -> log.debug("추천 사용자 목록 조회 완료: userId={}", userId));
    }

    /**
     * 사용자 차단
     */
    public Mono<Void> blockUser(Long blockerId, Long blockedId) {
        log.debug("사용자 차단 시작: blockerId={}, blockedId={}", blockerId, blockedId);
        
        // 자기 자신은 차단할 수 없음
        if (blockerId.equals(blockedId)) {
            return Mono.error(new RuntimeException("자신을 차단할 수 없습니다."));
        }
        
        // 사용자 존재 확인
        return userRepository.existsById(blockedId)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new RuntimeException("차단할 사용자가 존재하지 않습니다."));
                    }
                    
                    // 이미 차단 중인지 확인
                    return userRepository.isBlocked(blockerId, blockedId)
                            .flatMap(isBlocked -> {
                                if (isBlocked) {
                                    return Mono.empty(); // 이미 차단 중이면 아무 작업도 하지 않음
                                }
                                
                                // 차단 처리 및 팔로우 관계 제거
                                return userRepository.blockUser(blockerId, blockedId)
                                        .then(userRepository.unfollow(blockerId, blockedId))
                                        .then(userRepository.unfollow(blockedId, blockerId));
                            });
                })
                .doOnSuccess(result -> log.info("사용자 차단 성공: blockerId={}, blockedId={}", blockerId, blockedId));
    }

    /**
     * 사용자 차단 해제
     */
    public Mono<Void> unblockUser(Long blockerId, Long blockedId) {
        log.debug("사용자 차단 해제 시작: blockerId={}, blockedId={}", blockerId, blockedId);
        
        return userRepository.unblockUser(blockerId, blockedId)
                .doOnSuccess(result -> log.info("사용자 차단 해제 성공: blockerId={}, blockedId={}", blockerId, blockedId));
    }

    /**
     * 차단한 사용자 목록 조회
     */
    public Flux<User> getBlockedUsers(Long userId) {
        log.debug("차단 사용자 목록 조회: userId={}", userId);
        
        return userRepository.findBlockedUsers(userId)
                .doOnComplete(() -> log.debug("차단 사용자 목록 조회 완료: userId={}", userId));
    }
}