package kwh.PublicCookedFood.board.service;

import org.springframework.transaction.annotation.Transactional;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardImage;
import kwh.PublicCookedFood.board.domain.Images;
import kwh.PublicCookedFood.board.domain.Images.ImageStatus;
import kwh.PublicCookedFood.board.repository.BoardImageRepository;
import kwh.PublicCookedFood.board.repository.ImagesRepository;
import kwh.PublicCookedFood.config.properties.StorageProperties;
import kwh.PublicCookedFood.storage.StorageCategory;
import kwh.PublicCookedFood.storage.StorageException;
import kwh.PublicCookedFood.storage.StoragePathUtils;
import kwh.PublicCookedFood.storage.StorageService;
import kwh.PublicCookedFood.storage.StoredResource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");
    private static final Set<ImageStatus> DOWNLOADABLE_IMAGE_STATUSES = Set.of(ImageStatus.ATTACHED);
    private static final int MAX_ORIGINAL_FILENAME_LENGTH = 255;
    private static final int MAX_SAVED_FILENAME_LENGTH = 255;
    private static final int MAX_IMAGE_URL_LENGTH = 500;
    private static final int MAX_CONTENT_TYPE_LENGTH = 100;
    private static final Set<ImageStatus> LINKABLE_IMAGE_STATUSES = Set.of(ImageStatus.TEMP, ImageStatus.ATTACHED);
    private static final String ORIGINAL_IMAGE_DIRECTORY = "original";
    private static final String ORIGINAL_IMAGE_SUFFIX = "_orig";
    private final StorageService storageService;
    private final ImagesRepository imagesRepository;
    private final BoardImageRepository boardImageRepository;
    private final ImageSchemaService imageSchemaService;
    private final StorageProperties storageProperties;

    @Transactional
    public String uploadTempImage(MultipartFile file) {
        validateImageFile(file);
        imageSchemaService.ensureLegacyImagesSchemaCompatibleIfEnabled();
        StoredResource stored = storageService.store(file, StorageCategory.IMAGE);
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
            storageService.delete(StorageCategory.IMAGE, stored.savedFilename());
            if (isPostIdNullConstraintViolation(e)) {
                throw new StorageException("DB 스키마 오류: images.post_id가 NOT NULL 입니다. post_id를 NULL 허용으로 변경해야 합니다.", e);
            }
            throw new StorageException("이미지 메타데이터 저장 중 제약조건 오류가 발생했습니다.", e);
        } catch (RuntimeException e) {
            storageService.delete(StorageCategory.IMAGE, stored.savedFilename());
            throw new StorageException("이미지 메타데이터 저장에 실패했습니다.", e);
        }
    }

    @Transactional
    public void syncBoardImages(Board board) {
        syncBoardImages(board, board.getContents());
    }

    @Transactional
    public void syncBoardImages(Board board, String htmlContent) {
        imageSchemaService.ensureBoardImageSchemaCompatibleIfEnabled();
        Set<String> currentImageUrls = extractLocalImageUrls(htmlContent);
        List<BoardImage> boardImages = boardImageRepository.findAllByBoardWithImage(board);
        Map<String, BoardImage> boardImageByUrl = boardImages.stream()
                .filter(boardImage -> boardImage.getImage() != null && boardImage.getImage().getImgUrl() != null)
                .collect(Collectors.toMap(boardImage -> boardImage.getImage().getImgUrl(),
                        Function.identity(), (left, right) -> left));

        for (String imageUrl : currentImageUrls) {
            if (boardImageByUrl.containsKey(imageUrl)) {
                continue;
            }
            imagesRepository.findByImgUrlAndStatusIn(imageUrl, LINKABLE_IMAGE_STATUSES)
                    .ifPresent(image -> {
                        image.attach();
                        boardImageRepository.findByBoardAndImage(board, image)
                                .orElseGet(() -> boardImageRepository.save(BoardImage.of(board, image)));
                    });
        }

        for (BoardImage boardImage : boardImages) {
            Images image = boardImage.getImage();
            if (image == null) {
                boardImageRepository.delete(boardImage);
                continue;
            }
            if (currentImageUrls.contains(image.getImgUrl())) {
                continue;
            }
            boardImageRepository.delete(boardImage);
            cleanupImageIfUnlinked(image);
        }
    }

    @Transactional
    public void deleteBoardImages(Board board) {
        imageSchemaService.ensureBoardImageSchemaCompatibleIfEnabled();
        List<BoardImage> boardImages = boardImageRepository.findAllByBoardWithImage(board);
        for (BoardImage boardImage : boardImages) {
            Images image = boardImage.getImage();
            boardImageRepository.delete(boardImage);
            if (image != null) {
                cleanupImageIfUnlinked(image);
            }
        }
    }

    @Transactional
    public int deleteStaleTempImages(LocalDateTime cutoff, int batchSize) {
        imageSchemaService.ensureBoardImageSchemaCompatibleIfEnabled();
        if (cutoff == null || batchSize <= 0) {
            return 0;
        }

        Pageable pageable = PageRequest.of(0, batchSize);
        List<Images> staleTempImages = imagesRepository.findStaleImages(ImageStatus.TEMP, cutoff, pageable);

        int deletedCount = 0;
        for (Images staleImage : staleTempImages) {
            if (boardImageRepository.existsByImage(staleImage)) {
                continue;
            }
            int affected = imagesRepository.deleteByIdAndStatus(staleImage.getId(), ImageStatus.TEMP);
            if (affected > 0) {
                storageService.delete(StorageCategory.IMAGE, staleImage.getSavedFilename());
                deletedCount++;
            }
        }
        return deletedCount;
    }

    @Transactional(readOnly = true)
    public Optional<ImageDownloadResource> findOriginalImageForDownload(String imageUrl) {
        Optional<String> normalizedUrl = normalizeLocalImageUrl(imageUrl);
        if (normalizedUrl.isEmpty()) {
            return Optional.empty();
        }

        Optional<Images> imageOptional = imagesRepository.findByImgUrlAndStatusIn(normalizedUrl.get(), DOWNLOADABLE_IMAGE_STATUSES);
        if (imageOptional.isEmpty()) {
            return Optional.empty();
        }

        Images image = imageOptional.get();
        Path uploadRootPath = StoragePathUtils.resolveUploadPath(storageProperties.dir());
        Path imageDirectory = uploadRootPath.resolve(StorageCategory.IMAGE.getDirectoryName()).normalize();
        Path originalPath = findOriginalImagePath(imageDirectory, image.getSavedFilename(), image.getOriginalFilename());
        Path downloadPath = originalPath != null ? originalPath : resolveDisplayImagePath(uploadRootPath, imageDirectory, image.getSavedFilename());
        if (downloadPath == null || !Files.isRegularFile(downloadPath)) {
            return Optional.empty();
        }

        String downloadFilename = buildDownloadFilename(image.getOriginalFilename(), downloadPath);
        String contentType = resolveContentType(downloadPath, image.getContentType());
        long fileSize = resolveFileSize(downloadPath);
        return Optional.of(new ImageDownloadResource(downloadPath, downloadFilename, contentType, fileSize));
    }

    @Transactional(readOnly = true)
    public Optional<BoardImagesZipResource> findBoardImagesForZip(Long boardId, String htmlContent) {
        if (boardId == null) {
            return Optional.empty();
        }

        Set<String> imageUrls = extractLocalImageUrls(htmlContent);
        if (imageUrls.isEmpty()) {
            return Optional.empty();
        }

        List<ZipImageEntry> zipEntries = new ArrayList<>();
        Set<Path> includedPaths = new HashSet<>();
        Map<String, Integer> filenameSequence = new HashMap<>();

        for (String imageUrl : imageUrls) {
            Optional<ImageDownloadResource> downloadResourceOptional = findOriginalImageForDownload(imageUrl);
            if (downloadResourceOptional.isEmpty()) {
                continue;
            }

            ImageDownloadResource downloadResource = downloadResourceOptional.get();
            Path normalizedPath = downloadResource.filePath().toAbsolutePath().normalize();
            if (!Files.isRegularFile(normalizedPath) || !includedPaths.add(normalizedPath)) {
                continue;
            }

            String entryName = resolveUniqueZipEntryName(downloadResource.downloadFilename(), filenameSequence);
            zipEntries.add(new ZipImageEntry(normalizedPath, entryName));
        }

        if (zipEntries.isEmpty()) {
            return Optional.empty();
        }

        String zipFilename = "board-" + boardId + "-images.zip";
        return Optional.of(new BoardImagesZipResource(zipFilename, List.copyOf(zipEntries)));
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

    private Set<String> extractLocalImageUrls(String htmlContent) {
        if (htmlContent == null || htmlContent.isBlank()) {
            return Set.of();
        }

        Document document = Jsoup.parse(htmlContent);
        Set<String> imageUrls = new LinkedHashSet<>();
        for (Element imageTag : document.select("img[src]")) {
            String src = imageTag.attr("src");
            normalizeLocalImageUrl(src).ifPresent(imageUrls::add);
        }
        return imageUrls;
    }

    private Optional<String> normalizeLocalImageUrl(String src) {
        if (src == null || src.isBlank()) {
            return Optional.empty();
        }

        String trimmedSrc = src.trim();
        if (trimmedSrc.startsWith(StorageCategory.IMAGE.getUrlPrefix())) {
            return Optional.of(trimmedSrc);
        }

        if (trimmedSrc.startsWith("http://") || trimmedSrc.startsWith("https://")) {
            try {
                String path = URI.create(trimmedSrc).getPath();
                if (path != null && path.startsWith(StorageCategory.IMAGE.getUrlPrefix())) {
                    return Optional.of(path);
                }
            } catch (IllegalArgumentException ignored) {
                return Optional.empty();
            }
        }

        return Optional.empty();
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

    private void cleanupImageIfUnlinked(Images image) {
        if (boardImageRepository.existsByImage(image)) {
            return;
        }
        image.markDeleted();
        storageService.delete(StorageCategory.IMAGE, image.getSavedFilename());
    }

    private boolean isPostIdNullConstraintViolation(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase(Locale.ROOT);
                if (normalized.contains("post_id")
                        && (normalized.contains("cannot be null")
                        || normalized.contains("doesn't have a default value")
                        || normalized.contains("does not have a default value"))) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private Path findOriginalImagePath(Path imageDirectory, String savedFilename, String originalFilename) {
        String baseName = StoragePathUtils.extractBaseFilename(savedFilename);
        if (baseName.isBlank()) {
            return null;
        }

        Path originalDir = imageDirectory.resolve(ORIGINAL_IMAGE_DIRECTORY).normalize();
        if (!Files.isDirectory(originalDir)) {
            return null;
        }

        String originalExtension = extractExtension(originalFilename);
        if (!originalExtension.isBlank()) {
            Path candidate = StoragePathUtils.resolvePathUnderDirectory(originalDir, baseName + ORIGINAL_IMAGE_SUFFIX + originalExtension);
            if (candidate != null && Files.isRegularFile(candidate)) {
                return candidate;
            }
        }

        String pattern = baseName + ORIGINAL_IMAGE_SUFFIX + ".*";
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(originalDir, pattern)) {
            for (Path path : stream) {
                if (Files.isRegularFile(path)) {
                    return path;
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private Path resolveDisplayImagePath(Path uploadRootPath, Path imageDirectory, String savedFilename) {
        if (savedFilename == null || savedFilename.isBlank()) {
            return null;
        }
        Path imagePath = StoragePathUtils.resolvePathUnderDirectory(imageDirectory, savedFilename);
        if (imagePath != null && Files.isRegularFile(imagePath)) {
            return imagePath;
        }
        Path legacyPath = StoragePathUtils.resolvePathUnderDirectory(uploadRootPath, savedFilename);
        if (legacyPath != null && Files.isRegularFile(legacyPath)) {
            return legacyPath;
        }
        return null;
    }

    private String buildDownloadFilename(String originalFilename, Path downloadPath) {
        if (originalFilename != null && !originalFilename.isBlank()) {
            return originalFilename;
        }
        Path fileName = downloadPath.getFileName();
        return fileName == null ? "image" : fileName.toString();
    }

    private String resolveUniqueZipEntryName(String filename, Map<String, Integer> filenameSequence) {
        String sanitizedName = sanitizeZipEntryName(filename);
        String sequenceKey = sanitizedName.toLowerCase(Locale.ROOT);
        int sequence = filenameSequence.getOrDefault(sequenceKey, 0) + 1;
        filenameSequence.put(sequenceKey, sequence);
        if (sequence == 1) {
            return sanitizedName;
        }

        int dotIndex = sanitizedName.lastIndexOf('.');
        String baseName = dotIndex > 0 ? sanitizedName.substring(0, dotIndex) : sanitizedName;
        String extension = dotIndex > 0 ? sanitizedName.substring(dotIndex) : "";
        return baseName + " (" + sequence + ")" + extension;
    }

    private String sanitizeZipEntryName(String filename) {
        String candidate = filename == null ? "" : filename.trim();
        if (candidate.isBlank()) {
            candidate = "image";
        }

        candidate = candidate.replace('\\', '_')
                .replace('/', '_')
                .replace('\r', '_')
                .replace('\n', '_')
                .replace('\t', '_');

        while (candidate.contains("..")) {
            candidate = candidate.replace("..", "_");
        }

        if (candidate.isBlank() || ".".equals(candidate) || "..".equals(candidate)) {
            candidate = "image";
        }

        final int maxLength = 180;
        if (candidate.length() > maxLength) {
            int dotIndex = candidate.lastIndexOf('.');
            if (dotIndex > 0 && dotIndex < candidate.length() - 1) {
                String extension = candidate.substring(dotIndex);
                int baseMaxLength = maxLength - extension.length();
                if (baseMaxLength > 0) {
                    candidate = candidate.substring(0, baseMaxLength) + extension;
                } else {
                    candidate = candidate.substring(0, maxLength);
                }
            } else {
                candidate = candidate.substring(0, maxLength);
            }
        }

        return candidate;
    }

    private String resolveContentType(Path path, String fallbackContentType) {
        try {
            String detected = Files.probeContentType(path);
            if (detected != null && !detected.isBlank()) {
                return detected;
            }
        } catch (Exception ignored) {
            // Ignore and use fallback content type.
        }
        if (fallbackContentType != null && !fallbackContentType.isBlank()) {
            return fallbackContentType;
        }
        return "application/octet-stream";
    }

    private long resolveFileSize(Path path) {
        try {
            return Files.size(path);
        } catch (Exception ignored) {
            return -1L;
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex).toLowerCase(Locale.ROOT);
    }

    public record ImageDownloadResource(Path filePath, String downloadFilename, String contentType, long fileSize) {
    }

    public record BoardImagesZipResource(String zipFilename, List<ZipImageEntry> entries) {
    }

    public record ZipImageEntry(Path filePath, String entryName) {
    }
}
