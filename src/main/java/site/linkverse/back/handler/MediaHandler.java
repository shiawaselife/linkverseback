package site.linkverse.back.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import site.linkverse.back.dto.ApiResponse;
import site.linkverse.back.service.FileStorageService;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class MediaHandler {

    private final FileStorageService fileStorageService;

    /**
     * 파일 업로드 (압축 기능 포함)
     */
    public Mono<ServerResponse> uploadFile(ServerRequest request) {
        Long userId = (Long) request.attributes().get("userId");

        return request.multipartData()
                .map(multipartData -> multipartData.toSingleValueMap().get("file"))
                .cast(FilePart.class)
                .flatMap(fileStorageService::storeFile)
                .flatMap(fileInfo -> {
                    log.info("파일 업로드 완료 - 사용자: {}, 파일: {}, 압축: {}",
                            userId, fileInfo.get("originalFilename"), fileInfo.get("isCompressed"));

                    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                            ApiResponse.<Map<String, Object>>builder()
                                    .success(true)
                                    .message("파일이 업로드되었습니다" +
                                            (Boolean.TRUE.equals(fileInfo.get("isCompressed")) ? " (압축 적용)" : ""))
                                    .data(fileInfo)
                                    .build()
                    );
                })
                .onErrorResume(e -> {
                    log.error("파일 업로드 실패 - 사용자: {}, 오류: {}", userId, e.getMessage());
                    return ServerResponse.badRequest().bodyValue(
                            ApiResponse.builder()
                                    .success(false)
                                    .message("파일 업로드 실패: " + e.getMessage())
                                    .build()
                    );
                });
    }

    /**
     * 파일 다운로드 (압축 해제 포함)
     */
    public Mono<ServerResponse> downloadFile(ServerRequest request) {
        String filename = request.pathVariable("filename");

        return fileStorageService.readFile(filename)
                .flatMap(fileData -> {
                    // 원본 파일명 추출 (압축된 경우 .gz 제거)
                    String displayFilename = filename.endsWith(".gz") ?
                            filename.substring(0, filename.length() - 3) : filename;

                    ByteArrayResource resource = new ByteArrayResource(fileData);

                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_OCTET_STREAM)
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                    "attachment; filename=\"" + displayFilename + "\"")
                            .contentLength(fileData.length)
                            .bodyValue(resource);
                })
                .onErrorResume(e -> {
                    log.error("파일 다운로드 실패 - 파일: {}, 오류: {}", filename, e.getMessage());
                    return ServerResponse.notFound().build();
                });
    }
}