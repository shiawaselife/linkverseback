package site.linkverse.back.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import site.linkverse.back.dto.MessageCreateDto;
import site.linkverse.back.dto.MessageDto;
import site.linkverse.back.dto.UserDto;
import site.linkverse.back.model.Message;
import site.linkverse.back.repository.MessageRepository;
import site.linkverse.back.repository.UserBlockRepository;
import site.linkverse.back.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final UserBlockRepository userBlockRepository;
    
    public Mono<MessageDto> sendMessage(Long senderId, MessageCreateDto messageCreateDto) {
        // 차단 확인
        Mono<Boolean> blockCheckMono;
        if (messageCreateDto.getGroupId() == null) {
            blockCheckMono = userBlockRepository.existsByBlockerIdAndBlockedId(messageCreateDto.getReceiverId(), senderId)
                .flatMap(isBlocked -> {
                    if (isBlocked) {
                        return Mono.error(new RuntimeException("상대방에게 메시지를 보낼 수 없습니다"));
                    }
                    return Mono.just(false);
                });
        } else {
            blockCheckMono = Mono.just(false);
        }
        
        return blockCheckMono
            .then(Mono.defer(() -> {
                Message message = Message.builder()
                    .senderId(senderId)
                    .receiverId(messageCreateDto.getReceiverId())
                    .groupId(messageCreateDto.getGroupId())
                    .content(messageCreateDto.getContent())
                    .mediaUrl(messageCreateDto.getMediaUrl())
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();
                    
                return messageRepository.save(message)
                    .flatMap(this::convertToDto);
            }));
    }
    
    public Flux<MessageDto> getConversation(Long userId, Long otherUserId, int page, int size) {
        return messageRepository.findConversation(userId, otherUserId, PageRequest.of(page, size))
            .flatMap(this::convertToDto);
    }
    
    public Flux<MessageDto> getGroupMessages(Long groupId, int page, int size) {
        return messageRepository.findByGroupIdOrderByCreatedAtDesc(groupId, PageRequest.of(page, size))
            .flatMap(this::convertToDto);
    }
    
    public Flux<UserDto> getRecentConversations(Long userId, int page, int size) {
        return messageRepository.findRecentConversationUserIds(userId, PageRequest.of(page, size))
            .flatMap(otherUserId -> userRepository.findById(otherUserId))
            .map(user -> UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .profileImage(user.getProfileImage())
                .build());
    }
    
    public Mono<Void> markAsRead(List<Long> messageIds) {
        return messageRepository.updateReadStatusByIds(messageIds);
    }
    
    private Mono<MessageDto> convertToDto(Message message) {
        MessageDto.MessageDtoBuilder messageDtoBuilder = MessageDto.builder()
            .id(message.getId())
            .senderId(message.getSenderId())
            .receiverId(message.getReceiverId())
            .groupId(message.getGroupId())
            .content(message.getContent())
            .mediaUrl(message.getMediaUrl())
            .isRead(message.isRead())
            .createdAt(message.getCreatedAt());
            
        Mono<UserDto> senderMono = userRepository.findById(message.getSenderId())
            .map(user -> UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .profileImage(user.getProfileImage())
                .build());
                
        Mono<UserDto> receiverMono = message.getReceiverId() != null
            ? userRepository.findById(message.getReceiverId())
                .map(user -> UserDto.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .profileImage(user.getProfileImage())
                    .build())
            : Mono.empty();
            
        return Mono.zip(senderMono, receiverMono.defaultIfEmpty(null))
            .map(tuple -> {
                UserDto sender = tuple.getT1();
                UserDto receiver = tuple.getT2();
                
                return messageDtoBuilder
                    .sender(sender)
                    .receiver(receiver)
                    .build();
            });
    }
}