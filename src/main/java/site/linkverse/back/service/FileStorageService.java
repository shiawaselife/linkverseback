package site.linkverse.back.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import site.linkverse.back.util.CompressionUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    @Value("${file.upload.path:uploads}")
    private String uploadPath;

    @Value("${file.compression.enabled:true}")
    private boolean compressionEnabled;

    private final CompressionUtil compressionUtil;

    public Mono<Map<String, Object>> storeFile(FilePart filePart) {
        return collectFileData(filePart)
                .flatMap(fileData -> processAndStoreFile(fileData, filePart));
    }

    /**
     * 파일 데이터 수집
     */
    private Mono<byte[]> collectFileData(FilePart filePart) {
        return filePart.content()
                .map(DataBuffer::asByteBuffer)
                .map(byteBuffer -> {
                    byte[] bytes = new byte[byteBuffer.remaining()];
                    byteBuffer.get(bytes);
                    return bytes;
                })
                .reduce(this::combineByteArrays);
    }

    /**
     * 파일 처리 및 저장
     */
    private Mono<Map<String, Object>> processAndStoreFile(byte[] fileData, FilePart filePart) {
        return Mono.fromCallable(() -> {
            try {
                String originalFilename = filePart.filename();
                String mimeType = filePart.headers().getContentType() != null
                        ? filePart.headers().getContentType().toString()
                        : "application/octet-stream";

                // 디렉토리 생성
                createDirectoriesIfNotExists();

                // 고유 파일명 생성
                String uniqueFilename = generateUniqueFilename(originalFilename);

                byte[] finalData = fileData;
                boolean isCompressed = false;
                long originalSize = fileData.length;

                // 압축 여부 결정 및 압축 수행
                if (compressionEnabled && compressionUtil.shouldCompress(mimeType, fileData.length)) {
                    try {
                        byte[] compressedData = compressionUtil.compressFile(fileData);

                        // 압축 효과가 있는 경우만 압축된 파일 사용
                        double compressionRatio = (double)compressedData.length / fileData.length;
                        if (compressionRatio < 0.95) { // 5% 이상 압축된 경우만
                            finalData = compressedData;
                            isCompressed = true;
                            uniqueFilename += ".gz";

                            log.info("파일 압축 적용: {} → {} bytes ({}% 절약)",
                                    originalSize, finalData.length,
                                    Math.round((1 - compressionRatio) * 100));
                        } else {
                            log.info("압축 효과가 미미하여 원본 파일 사용: {}", originalFilename);
                        }
                    } catch (Exception e) {
                        log.warn("파일 압축 실패, 원본 파일 사용: {}", e.getMessage());
                    }
                }

                // 파일 저장
                Path filePath = Paths.get(uploadPath, uniqueFilename);
                Files.write(filePath, finalData);

                // 응답 데이터 구성
                Map<String, Object> result = new HashMap<>();
                result.put("filename", uniqueFilename);
                result.put("originalFilename", originalFilename);
                result.put("originalSize", originalSize);
                result.put("storedSize", finalData.length);
                result.put("isCompressed", isCompressed);
                result.put("mimeType", mimeType);
                result.put("url", "/api/media/" + uniqueFilename);

                if (isCompressed) {
                    double savedPercent = Math.round((1.0 - (double)finalData.length / originalSize) * 100);
                    result.put("compressionRatio", savedPercent + "% 절약");
                }

                return result;

            } catch (IOException e) {
                throw new RuntimeException("파일 저장 중 오류가 발생했습니다", e);
            }
        });
    }

    public Mono<Path> getFilePath(String filename) {
        Path path = Paths.get(uploadPath, filename);
        return Mono.just(path);
    }

    /**
     * 압축된 파일인지 확인하고 적절히 처리
     */
    public Mono<byte[]> readFile(String filename) {
        return Mono.fromCallable(() -> {
            try {
                Path filePath = Paths.get(uploadPath, filename);
                byte[] fileData = Files.readAllBytes(filePath);

                // .gz 확장자가 있으면 압축 해제
                if (filename.endsWith(".gz")) {
                    return compressionUtil.decompressFile(fileData);
                }

                return fileData;
            } catch (IOException e) {
                throw new RuntimeException("파일 읽기 중 오류가 발생했습니다", e);
            }
        });
    }

    // 유틸리티 메서드들
    private void createDirectoriesIfNotExists() throws IOException {
        Path dirPath = Paths.get(uploadPath);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }
    }

    private String generateUniqueFilename(String originalFilename) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String extension = getFileExtension(originalFilename);

        return timestamp + "_" + uuid + "." + extension;
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return filename.substring(lastDotIndex + 1);
        }
        return "bin";
    }

    private byte[] combineByteArrays(byte[] array1, byte[] array2) {
        byte[] result = new byte[array1.length + array2.length];
        System.arraycopy(array1, 0, result, 0, array1.length);
        System.arraycopy(array2, 0, result, array1.length, array2.length);
        return result;
    }
}