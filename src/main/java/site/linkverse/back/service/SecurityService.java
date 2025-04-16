package site.linkverse.back.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import site.linkverse.back.dto.BlockUserDto;
import site.linkverse.back.dto.ReportDto;
import site.linkverse.back.dto.UserDto;
import site.linkverse.back.enums.ReportStatus;
import site.linkverse.back.enums.ReportType;
import site.linkverse.back.model.Report;
import site.linkverse.back.model.UserBlock;
import site.linkverse.back.repository.*;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SecurityService {
    private final UserBlockRepository userBlockRepository;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    
    public Mono<Void> blockUser(Long blockerId, BlockUserDto blockUserDto) {
        if (blockerId.equals(blockUserDto.getBlockedId())) {
            return Mono.error(new RuntimeException("자기 자신을 차단할 수 없습니다"));
        }
        
        return userRepository.findById(blockUserDto.getBlockedId())
            .switchIfEmpty(Mono.error(new RuntimeException("사용자를 찾을 수 없습니다")))
            .flatMap(blockedUser -> {
                return userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockUserDto.getBlockedId())
                    .flatMap(exists -> {
                        if (exists) {
                            return Mono.error(new RuntimeException("이미 차단한 사용자입니다"));
                        }
                        
                        UserBlock userBlock = UserBlock.builder()
                            .blockerId(blockerId)
                            .blockedId(blockUserDto.getBlockedId())
                            .createdAt(LocalDateTime.now())
                            .build();
                            
                        return userBlockRepository.save(userBlock).then();
                    });
            });
    }
    
    public Mono<Void> unblockUser(Long blockerId, Long blockedId) {
        return userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)
            .flatMap(exists -> {
                if (!exists) {
                    return Mono.error(new RuntimeException("차단하지 않은 사용자입니다"));
                }
                
                return userBlockRepository.deleteByBlockerIdAndBlockedId(blockerId, blockedId);
            });
    }
    
    public Flux<UserDto> getBlockedUsers(Long blockerId) {
        return userBlockRepository.findByBlockerId(blockerId)
            .flatMap(userBlock -> userRepository.findById(userBlock.getBlockedId()))
            .map(user -> UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .profileImage(user.getProfileImage())
                .build());
    }

    public Mono<Void> reportContent(Long reporterId, ReportDto reportDto) {
        // 이미 신고한 내역이 있는지 확인
        return reportRepository.existsByReporterIdAndReportedIdAndReportType(reporterId, reportDto.getReportedId(), reportDto.getReportType())
                .flatMap(alreadyReported -> {
                    if (alreadyReported) {
                        return Mono.error(new RuntimeException("이미 신고한 내용입니다"));
                    }

                    // 신고 대상이 존재하는지 확인
                    Mono<Boolean> targetExistsMono;
                    if (reportDto.getReportType() == ReportType.USER) {
                        targetExistsMono = userRepository.existsById(reportDto.getReportedId());
                    } else if (reportDto.getReportType() == ReportType.POST) {
                        targetExistsMono = postRepository.existsById(reportDto.getReportedId());
                    } else if (reportDto.getReportType() == ReportType.COMMENT) {
                        targetExistsMono = commentRepository.existsById(reportDto.getReportedId());
                    } else {
                        return Mono.error(new RuntimeException("잘못된 신고 유형입니다"));
                    }

                    return targetExistsMono.flatMap(targetExists -> {
                        if (!targetExists) {
                            return Mono.error(new RuntimeException("신고 대상을 찾을 수 없습니다"));
                        }

                        Report report = Report.builder()
                                .reporterId(reporterId)
                                .reportedId(reportDto.getReportedId())
                                .reportType(reportDto.getReportType())
                                .reason(reportDto.getReason())
                                .status(ReportStatus.PENDING)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();

                        return reportRepository.save(report).then();
                    });
                });
    }
}