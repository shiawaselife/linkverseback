package site.linkverse.back.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import site.linkverse.back.enums.ReportStatus;
import site.linkverse.back.enums.ReportType;
import site.linkverse.back.model.Report;

@Repository
public interface ReportRepository extends R2dbcRepository<Report, Long> {
    Flux<Report> findByStatusOrderByCreatedAtAsc(ReportStatus status, Pageable pageable);
    Mono<Boolean> existsByReporterIdAndReportedIdAndReportType(Long reporterId, Long reportedId, ReportType reportType);
}