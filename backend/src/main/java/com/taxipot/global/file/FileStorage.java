package com.taxipot.global.file;

import com.taxipot.global.config.AppProperties;
import com.taxipot.global.error.BusinessException;
import com.taxipot.global.error.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * 캡처본(user)·영수증(settlement) 업로드 공통. 명세 규칙:
 * jpg·png만, 10MB 이하, app.upload-dir 아래 UUID 파일명, DB에는 상대 경로만, 트랜잭션 롤백 시 파일 삭제.
 *
 * 저장: String path = fileStorage.save(receiptImage, "receipts");   // @Transactional 메서드 안에서
 * 조회: return fileStorage.loadImage(pot.getReceiptImgPath());
 * 하위 폴더 이름은 "captures", "receipts" 두 개로 고정.
 */
@Slf4j
@Component
public class FileStorage {

    private final Path root;

    public FileStorage(AppProperties appProperties) {
        this.root = Path.of(appProperties.uploadDir()).toAbsolutePath().normalize();
    }

    public String save(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "파일이 비어 있습니다.", null);
        }
        String ext = detectImageExtension(file);
        String relative = subDir + "/" + UUID.randomUUID() + "." + ext;
        Path target = resolve(relative);
        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("파일 저장 실패", e);
        }
        deleteOnRollback(relative);
        return relative;
    }

    /** 이미지 조회 API용. 경로가 null이거나 파일이 없으면 404 NOT_FOUND */
    public StoredImage loadImage(String relativePath) {
        if (relativePath == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        Path path = resolve(relativePath);
        if (!Files.exists(path)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        Resource resource = new FileSystemResource(path);
        return new StoredImage(resource, relativePath.endsWith(".png") ? "image/png" : "image/jpeg");
    }

    /** 실패해도 예외를 던지지 않고 false (스케줄러 10번: 실패 시 DB 값 유지 후 다음 실행 때 재시도) */
    public boolean delete(String relativePath) {
        try {
            return Files.deleteIfExists(resolve(relativePath));
        } catch (IOException e) {
            log.warn("파일 삭제 실패: {}", relativePath, e);
            return false;
        }
    }

    private Path resolve(String relative) {
        Path path = root.resolve(relative).normalize();
        if (!path.startsWith(root)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        return path;
    }

    /** 확장자·Content-Type은 앱이 바꿔 보낼 수 있으므로 파일 앞부분(매직 넘버)으로 판별 */
    private String detectImageExtension(MultipartFile file) {
        byte[] head = new byte[8];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.readNBytes(head, 0, 8);
        } catch (IOException e) {
            throw new IllegalStateException("파일 읽기 실패", e);
        }
        if (read >= 2 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8) {
            return "jpg";
        }
        if (read >= 4 && (head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G') {
            return "png";
        }
        throw new BusinessException(ErrorCode.INVALID_INPUT, "jpg·png 이미지만 올릴 수 있습니다.", null);
    }

    private void deleteOnRollback(String relative) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            log.warn("트랜잭션 밖에서 파일 저장됨 — 롤백 시 자동 삭제 안 됨: {}", relative);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    delete(relative);
                }
            }
        });
    }
}
