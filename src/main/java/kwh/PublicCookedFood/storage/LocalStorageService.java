package kwh.PublicCookedFood.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
public class LocalStorageService implements StorageService {

    private final Path rootPath;

    public LocalStorageService(@Value("${file.dir}") String fileDir) {
        this.rootPath = resolveUploadPath(fileDir);
    }

    @Override
    public StoredResource store(MultipartFile file, StorageCategory category) {
        if (file == null || file.isEmpty()) {
            throw new StorageException("저장할 파일이 비어 있습니다.");
        }

        String originalFilename = file.getOriginalFilename();
        String fileExtension = extractFileExtension(originalFilename);
        String savedFilename = UUID.randomUUID() + fileExtension;

        Path categoryDir = rootPath.resolve(category.getDirectoryName()).normalize();
        Path targetPath = categoryDir.resolve(savedFilename).normalize();

        try {
            Files.createDirectories(categoryDir);
            file.transferTo(targetPath);
        } catch (IOException e) {
            throw new StorageException("파일 저장에 실패했습니다.", e);
        }

        String contentType = file.getContentType() == null ? "" : file.getContentType();
        long fileSize = file.getSize();
        String resourceUrl = category.getUrlPrefix() + savedFilename;

        return new StoredResource(originalFilename, savedFilename, resourceUrl, contentType, fileSize);
    }

    @Override
    public void delete(StorageCategory category, String savedFilename) {
        if (savedFilename == null || savedFilename.isBlank()) {
            return;
        }

        Path categoryDir = rootPath.resolve(category.getDirectoryName()).normalize();
        Path targetPath = categoryDir.resolve(savedFilename).normalize();
        if (category == StorageCategory.IMAGE && !Files.exists(targetPath)) {
            targetPath = rootPath.resolve(savedFilename).normalize(); // 기존 이미지 저장 경로 호환
        }
        try {
            Files.deleteIfExists(targetPath);
        } catch (IOException e) {
            log.warn("파일 삭제에 실패했습니다. path={}", targetPath, e);
        }
    }

    private String extractFileExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        return extension.toLowerCase(Locale.ROOT);
    }

    private Path resolveUploadPath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            throw new IllegalArgumentException("file.dir 값이 비어 있습니다.");
        }

        String normalized = rawPath.trim();
        if (normalized.matches("^/[A-Za-z]:/.*")) {
            normalized = normalized.substring(1);
        }
        return Path.of(normalized).toAbsolutePath().normalize();
    }
}
