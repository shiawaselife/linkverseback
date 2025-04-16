package site.linkverse.back.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import site.linkverse.back.config.JwtUtil;
import site.linkverse.back.dto.*;
import site.linkverse.back.enums.VisibilityType;
import site.linkverse.back.model.User;
import site.linkverse.back.repository.FollowRepository;
import site.linkverse.back.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TransactionalOperator transactionalOperator;

    public Mono<UserDto> registerUser(UserRegistrationDto registrationDto) {
        return userRepository.existsByEmail(registrationDto.getEmail())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new RuntimeException("이메일이 이미 사용 중입니다"));
                    }

                    User user = User.builder()
                            .email(registrationDto.getEmail())
                            .password(passwordEncoder.encode(registrationDto.getPassword()))
                            .username(registrationDto.getUsername())
                            .profileImage(registrationDto.getProfileImage())
                            .bio(registrationDto.getBio())
                            .profileVisibility(VisibilityType.PUBLIC)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .emailVerified(false)
                            .build();

                    return userRepository.save(user)
                            .map(this::convertToDto);
                });
    }

    public Mono<AuthResponse> login(UserLoginDto loginDto) {
        return userRepository.findByEmail(loginDto.getEmail())
                .filter(user -> passwordEncoder.matches(loginDto.getPassword(), user.getPassword()))
                .switchIfEmpty(Mono.error(new RuntimeException("아이디 또는 비밀번호가 잘못되었습니다")))
                .flatMap(user -> {
                    user.setLastLoginAt(LocalDateTime.now());
                    return userRepository.save(user)
                            .map(updatedUser -> {
                                String token = jwtUtil.generateToken(updatedUser);
                                String refreshToken = jwtUtil.generateRefreshToken(updatedUser);
                                return AuthResponse.builder()
                                        .token(token)
                                        .refreshToken(refreshToken)
                                        .user(convertToDto(updatedUser))
                                        .build();
                            });
                });
    }

    public Mono<UserDto> getUserById(Long userId) {
        return userRepository.findById(userId)
                .map(this::convertToDto);
    }

    public Mono<UserDto> updateUser(Long userId, UserUpdateDto updateDto) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new RuntimeException("사용자를 찾을 수 없습니다")))
                .flatMap(user -> {
                    user.setUsername(updateDto.getUsername());
                    user.setProfileImage(updateDto.getProfileImage());
                    user.setBio(updateDto.getBio());
                    user.setProfileVisibility(updateDto.getProfileVisibility());
                    user.setNotificationSettings(updateDto.getNotificationSettings());
                    user.setUpdatedAt(LocalDateTime.now());

                    return userRepository.save(user)
                            .map(this::convertToDto);
                });
    }

    public Mono<Void> updatePassword(Long userId, PasswordUpdateDto passwordUpdateDto) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new RuntimeException("사용자를 찾을 수 없습니다")))
                .flatMap(user -> {
                    if (!passwordEncoder.matches(passwordUpdateDto.getCurrentPassword(), user.getPassword())) {
                        return Mono.error(new RuntimeException("현재 비밀번호가 일치하지 않습니다"));
                    }

                    if (!passwordUpdateDto.getNewPassword().equals(passwordUpdateDto.getConfirmPassword())) {
                        return Mono.error(new RuntimeException("새 비밀번호가 일치하지 않습니다"));
                    }

                    user.setPassword(passwordEncoder.encode(passwordUpdateDto.getNewPassword()));
                    user.setUpdatedAt(LocalDateTime.now());

                    return userRepository.save(user).then();
                });
    }

    public Flux<UserDto> searchUsers(String keyword, int page, int size) {
        return userRepository.searchByKeyword("%" + keyword + "%", PageRequest.of(page, size))
                .flatMap(user -> followRepository.countByFollowingId(user.getId())
                        .map(followersCount -> {
                            UserDto dto = convertToDto(user);
                            dto.setFollowersCount(followersCount.intValue());
                            return dto;
                        }));
    }

    private UserDto convertToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .profileImage(user.getProfileImage())
                .bio(user.getBio())
                .profileVisibility(user.getProfileVisibility())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .emailVerified(user.isEmailVerified())
                .build();
    }
}