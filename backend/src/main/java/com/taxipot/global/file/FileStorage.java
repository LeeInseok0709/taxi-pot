package com.taxipot.global.file;

import com.taxipot.global.config.AppProperties;
import com.taxipot.global.error.Errors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;

/**
 * 캡처본·영수증 업로드 공용 (명세: 서버 로컬 폴더, UUID 파일명, DB 에는 상대 경로만).
 * <ul>
 *   <li>jpg·png 만 (확장자 + 파일 앞부분 시그니처 검사), 크기 제한은 spring.servlet.multipart</li>
 *   <li>@Transactional 안에서 호출하면, 트랜잭션이 롤백될 때 저장한 파일을 자동으로 지운다</li>
 * </ul>
 * <pre>
 * String path = fileStorage.saveImage(request.receiptImage(), FileStorage.DIR_RECEIPT + "/" + potId);
 * pot.claimFare(actualFare, path, now);
 * </pre>
 */
@Slf4j
@Component
public class FileStorage {

    public static final String DIR_CAPTURE = "captures";
    public static final String DIR_RECEIPT = "receipts";

    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    private final Path root;

    public FileStorage(AppProperties props) {
        this.root = Path.of(props.upload().dir()).toAbsolutePath().normalize();
    }

    /**
     * @param dir 저장 폴더 (명세: captures/{memberId}, receipts/{potId})
     * @return DB 에 저장할 상대 경로 (예: captures/12/uuid.jpg)
     */
    public String saveImage(MultipartFile file, String dir) {
        if (file == null || file.isEmpty()) {
            throw Errors.invalidInput("이미지 파일이 비어 있습니다.");
        }
        String ext = detectImageExtension(file);
        String relative = dir + "/" + UUID.randomUUID() + "." + ext;
        Path target = resolve(relative);
        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("파일 저장 실패: " + relative, e);
        }
        deleteOnRollback(relative);
        return relative;
    }

    public StoredFile load(String relativePath) {
        if (relativePath == null) {
            throw Errors.notFound("이미지");
        }
        Path path = resolve(relativePath);
        if (!Files.isRegularFile(path)) {
            throw Errors.notFound("이미지");
        }
        MediaType type = relativePath.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return new StoredFile(new FileSystemResource(path), type);
    }

    /** 즉시 삭제. 실패하면 false (스케줄러는 DB 값을 그대로 두고 다음에 재시도) */
    public boolean delete(String relativePath) {
        if (relativePath == null) {
            return true;
        }
        try {
            Files.deleteIfExists(resolve(relativePath));
            return true;
        } catch (IOException e) {
            log.warn("파일 삭제 실패: {}", relativePath, e);
            return false;
        }
    }

    /** 커밋된 뒤에 삭제 (예: 재청구로 영수증을 교체할 때 이전 파일) */
    public void deleteAfterCommit(String relativePath) {
        if (relativePath == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            delete(relativePath);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                delete(relativePath);
            }
        });
    }

    private void deleteOnRollback(String relativePath) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            log.warn("트랜잭션 밖에서 파일 저장: {} (롤백 시 자동 삭제 안 됨)", relativePath);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    delete(relativePath);
                }
            }
        });
    }

    private Path resolve(String relativePath) {
        Path path = root.resolve(relativePath).normalize();
        if (!path.startsWith(root)) {
            throw Errors.invalidInput("잘못된 파일 경로입니다.");
        }
        return path;
    }

    private static String detectImageExtension(MultipartFile file) {
        byte[] head;
        try (InputStream in = file.getInputStream()) {
            head = in.readNBytes(4);
        } catch (IOException e) {
            throw Errors.invalidInput("파일을 읽을 수 없습니다.");
        }
        if (startsWith(head, PNG)) {
            return "png";
        }
        if (startsWith(head, JPEG)) {
            return "jpg";
        }
        throw Errors.invalidInput("jpg·png 이미지만 올릴 수 있습니다.");
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        return data.length >= prefix.length && Arrays.equals(Arrays.copyOf(data, prefix.length), prefix);
    }
}
