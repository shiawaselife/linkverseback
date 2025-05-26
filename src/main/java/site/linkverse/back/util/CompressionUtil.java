package site.linkverse.back.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Component
@Slf4j
public class CompressionUtil {

    /**
     * 파일을 GZIP으로 압축
     */
    public byte[] compressFile(byte[] data) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzipOut = new GZIPOutputStream(baos)) {
            
            gzipOut.write(data);
            gzipOut.finish();
            
            byte[] compressed = baos.toByteArray();
            log.info("파일 압축 완료: {} bytes → {} bytes (압축률: {}%)", 
                data.length, compressed.length, 
                Math.round((1.0 - (double)compressed.length / data.length) * 100));
            
            return compressed;
        } catch (IOException e) {
            log.error("파일 압축 실패", e);
            throw new RuntimeException("파일 압축 중 오류가 발생했습니다", e);
        }
    }

    /**
     * GZIP 압축 해제
     */
    public byte[] decompressFile(byte[] compressedData) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
             GZIPInputStream gzipIn = new GZIPInputStream(bais);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzipIn.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("파일 압축 해제 실패", e);
            throw new RuntimeException("파일 압축 해제 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 압축이 효과적인지 판단
     * @param mimeType 파일의 MIME 타입
     * @param fileSize 파일 크기
     * @return 압축할지 여부
     */
    public boolean shouldCompress(String mimeType, long fileSize) {
        // 이미 압축된 파일들은 압축하지 않음
        if (mimeType.contains("zip") || mimeType.contains("gzip") || 
            mimeType.contains("jpeg") || mimeType.contains("jpg") || 
            mimeType.contains("png") || mimeType.contains("mp4") || 
            mimeType.contains("mp3")) {
            return false;
        }
        
        // 텍스트 파일이나 1KB 이상의 파일만 압축
        return (mimeType.startsWith("text/") || 
                mimeType.contains("json") || 
                mimeType.contains("xml") ||
                mimeType.contains("javascript") ||
                mimeType.contains("css")) && 
               fileSize > 1024; // 1KB 이상
    }
}