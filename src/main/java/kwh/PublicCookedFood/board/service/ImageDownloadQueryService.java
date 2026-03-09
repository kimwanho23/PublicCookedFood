package kwh.PublicCookedFood.board.service;

import kwh.PublicCookedFood.board.domain.Images;
import kwh.PublicCookedFood.board.domain.Images.ImageStatus;
import kwh.PublicCookedFood.board.repository.ImagesRepository;
import kwh.PublicCookedFood.config.properties.StorageProperties;
import kwh.PublicCookedFood.storage.StorageCategory;
import kwh.PublicCookedFood.storage.StoragePathUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ImageDownloadQueryService {

    private static final Set<ImageStatus> DOWNLOADABLE_IMAGE_STATUSES = Set.of(ImageStatus.ATTACHED);
    private static final String ORIGINAL_IMAGE_DIRECTORY = "original";
    private static final String ORIGINAL_IMAGE_SUFFIX = "_orig";

    private final ImagesRepository imagesRepository;
    private final StorageProperties storageProperties;
    private final ImageUrlSupport imageUrlSupport;

    @Transactional(readOnly = true)
    public Optional<ImageService.ImageDownloadResource> findOriginalImageForDownload(String imageUrl) {
        Optional<String> normalizedUrl = imageUrlSupport.normalizeLocalImageUrl(imageUrl);
        if (normalizedUrl.isEmpty()) {
            return Optional.empty();
        }

        Optional<Images> imageOptional = imagesRepository.findByImgUrlAndStatusIn(normalizedUrl.get(), DOWNLOADABLE_IMAGE_STATUSES);
        if (imageOptional.isEmpty()) {
            return Optional.empty();
        }

        Images image = imageOptional.get();
        Path uploadRootPath = StoragePathUtils.resolveUploadPath(storageProperties.dir());
        StorageCategory storageCategory = StorageCategory.resolveByUrl(image.getImgUrl())
                .orElse(StorageCategory.IMAGE);
        Path imageDirectory = uploadRootPath.resolve(storageCategory.getDirectoryName()).normalize();
        Path originalPath = findOriginalImagePath(imageDirectory, image.getSavedFilename(), image.getOriginalFilename());
        Path downloadPath = originalPath != null ? originalPath : resolveDisplayImagePath(uploadRootPath, imageDirectory, image.getSavedFilename());
        if (downloadPath == null || !Files.isRegularFile(downloadPath)) {
            return Optional.empty();
        }

        String downloadFilename = buildDownloadFilename(image.getOriginalFilename(), downloadPath);
        String contentType = resolveContentType(downloadPath, image.getContentType());
        long fileSize = resolveFileSize(downloadPath);
        return Optional.of(new ImageService.ImageDownloadResource(downloadPath, downloadFilename, contentType, fileSize));
    }

    @Transactional(readOnly = true)
    public Optional<ImageService.BoardImagesZipResource> findBoardImagesForZip(Long boardId, String htmlContent) {
        if (boardId == null) {
            return Optional.empty();
        }

        Set<String> imageUrls = imageUrlSupport.extractLocalImageUrls(htmlContent);
        if (imageUrls.isEmpty()) {
            return Optional.empty();
        }

        List<ImageService.ZipImageEntry> zipEntries = new ArrayList<>();
        Set<Path> includedPaths = new HashSet<>();
        Map<String, Integer> filenameSequence = new HashMap<>();

        for (String imageUrl : imageUrls) {
            Optional<ImageService.ImageDownloadResource> downloadResourceOptional = findOriginalImageForDownload(imageUrl);
            if (downloadResourceOptional.isEmpty()) {
                continue;
            }

            ImageService.ImageDownloadResource downloadResource = downloadResourceOptional.get();
            Path normalizedPath = downloadResource.filePath().toAbsolutePath().normalize();
            if (!Files.isRegularFile(normalizedPath) || !includedPaths.add(normalizedPath)) {
                continue;
            }

            String entryName = resolveUniqueZipEntryName(downloadResource.downloadFilename(), filenameSequence);
            zipEntries.add(new ImageService.ZipImageEntry(normalizedPath, entryName));
        }

        if (zipEntries.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new ImageService.BoardImagesZipResource(
                "board-" + boardId + "-images.zip",
                List.copyOf(zipEntries)
        ));
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
        } catch (IOException ignored) {
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

        int maxLength = 180;
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
        } catch (IOException ignored) {
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
        } catch (IOException ignored) {
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
}
