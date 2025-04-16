package site.linkverse.back.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import site.linkverse.back.dto.NotificationDto;
import site.linkverse.back.dto.UserDto;
import site.linkverse.back.enums.NotificationType;
import site.linkverse.back.model.Notification;
import site.linkverse.back.repository.NotificationRepository;
import site.linkverse.back.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    
    public Mono<NotificationDto> createNotification(Long userId, Long senderId, NotificationType type, Long referenceId, String content) {
        Notification notification = Notification.builder()
            .userId(userId)
            .senderId(senderId)
            .notificationType(type)
            .referenceId(referenceId)
            .content(content)
            .isRead(false)
            .createdAt(LocalDateTime.now())
            .build();
            
        return notificationRepository.save(notification)
            .flatMap(this::convertToDto);
    }
    
    public Flux<NotificationDto> getUserNotifications(Long userId, int page, int size) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
            .flatMap(this::convertToDto);
    }
    
    public Mono<Long> countUnreadNotifications(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }
    
    public Mono<Void> markAsRead(List<Long> notificationIds) {
        return notificationRepository.updateReadStatusByIds(notificationIds);
    }
    
    private Mono<NotificationDto> convertToDto(Notification notification) {
        NotificationDto.NotificationDtoBuilder notificationDtoBuilder = NotificationDto.builder()
            .id(notification.getId())
            .userId(notification.getUserId())
            .senderId(notification.getSenderId())
            .notificationType(notification.getNotificationType())
            .referenceId(notification.getReferenceId())
            .content(notification.getContent())
            .isRead(notification.isRead())
            .createdAt(notification.getCreatedAt());
            
        return userRepository.findById(notification.getSenderId())
            .map(user -> UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .profileImage(user.getProfileImage())
                .build())
            .map(sender -> notificationDtoBuilder
                .sender(sender)
                .build());
    }
}