package kwh.PublicCookedFood.board.service;

import kwh.PublicCookedFood.storage.ImageUploadCommand;
import kwh.PublicCookedFood.storage.Images;
import kwh.PublicCookedFood.storage.ImagesRepository;
import kwh.PublicCookedFood.storage.StorageCategory;
import kwh.PublicCookedFood.storage.StorageException;
import kwh.PublicCookedFood.storage.StorageService;
import kwh.PublicCookedFood.storage.StoredResource;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ImageTempUploadService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");
    private static final int MAX_ORIGINAL_FILENAME_LENGTH = 255;
    private static final int MAX_SAVED_FILENAME_LENGTH = 255;
    private static final int MAX_IMAGE_URL_LENGTH = 500;
    private static final int MAX_CONTENT_TYPE_LENGTH = 100;

    private final StorageService storageService;
    private final ImagesRepository imagesRepository;

    @Transactional
    public String uploadTempImage(ImageUploadCommand command) {
        MultipartFile file = command.file();
        validateImageFile(file);
        StorageCategory resolvedCategory = command.storageCategory();
        StoredResource stored = storageService.store(file, resolvedCategory);
        try {
            Images savedImage = Images.createTemporary(
                    limitLength(stored.originalFilename(), MAX_ORIGINAL_FILENAME_LENGTH),
                    limitLength(stored.savedFilename(), MAX_SAVED_FILENAME_LENGTH),
                    limitLength(stored.resourceUrl(), MAX_IMAGE_URL_LENGTH),
                    limitLength(stored.contentType(), MAX_CONTENT_TYPE_LENGTH),
                    stored.fileSize()
            );
            imagesRepository.saveAndFlush(savedImage);
            return stored.resourceUrl();
        } catch (DataIntegrityViolationException e) {
            storageService.delete(resolvedCategory, stored.savedFilename());
            throw new StorageException("이미지 메타데이터 저장 중 제약조건 오류가 발생했습니다.", e);
        } catch (RuntimeException e) {
            storageService.delete(resolvedCategory, stored.savedFilename());
            throw new StorageException("이미지 메타데이터 저장에 실패했습니다.", e);
        }
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어 있습니다.");
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || !originalFileName.contains(".")) {
            throw new IllegalArgumentException("잘못된 파일명입니다.");
        }

        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf(".")).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(fileExtension)) {
            throw new IllegalArgumentException("지원하지 않는 확장자입니다.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다.");
        }
    }

    private String limitLength(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (maxLength <= 0 || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

}
