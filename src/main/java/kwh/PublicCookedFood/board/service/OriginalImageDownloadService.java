package kwh.PublicCookedFood.board.service;

import kwh.PublicCookedFood.config.properties.StorageProperties;
import kwh.PublicCookedFood.storage.ImageDownloadQuery;
import kwh.PublicCookedFood.storage.ImageDownloadResource;
import kwh.PublicCookedFood.storage.Images;
import kwh.PublicCookedFood.storage.Images.ImageStatus;
import kwh.PublicCookedFood.storage.ImagesRepository;
import kwh.PublicCookedFood.storage.StorageCategory;
import kwh.PublicCookedFood.storage.StoragePathUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class OriginalImageDownloadService {

    private static final Set<ImageStatus> DOWNLOADABLE_IMAGE_STATUSES = Set.of(ImageStatus.ATTACHED);
    private static final String ORIGINAL_IMAGE_DIRECTORY = "original";
    private static final String ORIGINAL_IMAGE_SUFFIX = "_orig";

    private final ImagesRepository imagesRepository;
    private final StorageProperties storageProperties;
    private final ImageUrlSupport imageUrlSupport;

    @Transactional(readOnly = true)
    public Optional<ImageDownloadResource> findOriginalImageForDownload(ImageDownloadQuery query) {
        Optional<String> normalizedUrl = imageUrlSupport.normalizeLocalImageUrl(query.imageUrl());
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
        Optional<Path> downloadPath = findOriginalImagePath(imageDirectory, image.getSavedFilename(), image.getOriginalFilename())
                .or(() -> resolveDisplayImagePath(uploadRootPath, imageDirectory, image.getSavedFilename()));
        if (downloadPath.isEmpty() || !Files.isRegularFile(downloadPath.get())) {
            return Optional.empty();
        }

        Path resolvedDownloadPath = downloadPath.get();
        String downloadFilename = buildDownloadFilename(image.getOriginalFilename(), resolvedDownloadPath);
        String contentType = resolveContentType(resolvedDownloadPath, image.getContentType());
        long fileSize = resolveFileSize(resolvedDownloadPath);
        return Optional.of(new ImageDownloadResource(resolvedDownloadPath, downloadFilename, contentType, fileSize));
    }

    private Optional<Path> findOriginalImagePath(Path imageDirectory, String savedFilename, String originalFilename) {
        String baseName = StoragePathUtils.extractBaseFilename(savedFilename);
        if (baseName.isBlank()) {
            return Optional.empty();
        }

        Path originalDir = imageDirectory.resolve(ORIGINAL_IMAGE_DIRECTORY).normalize();
        if (!Files.isDirectory(originalDir)) {
            return Optional.empty();
        }

        String originalExtension = extractExtension(originalFilename);
        if (!originalExtension.isBlank()) {
            Path candidate = StoragePathUtils.resolvePathUnderDirectory(originalDir, baseName + ORIGINAL_IMAGE_SUFFIX + originalExtension);
            if (candidate != null && Files.isRegularFile(candidate)) {
                return Optional.of(candidate);
            }
        }

        String pattern = baseName + ORIGINAL_IMAGE_SUFFIX + ".*";
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(originalDir, pattern)) {
            for (Path path : stream) {
                if (Files.isRegularFile(path)) {
                    return Optional.of(path);
                }
            }
        } catch (IOException ignored) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    private Optional<Path> resolveDisplayImagePath(Path uploadRootPath, Path imageDirectory, String savedFilename) {
        if (savedFilename == null || savedFilename.isBlank()) {
            return Optional.empty();
        }

        Path imagePath = StoragePathUtils.resolvePathUnderDirectory(imageDirectory, savedFilename);
        if (imagePath != null && Files.isRegularFile(imagePath)) {
            return Optional.of(imagePath);
        }

        Path legacyPath = StoragePathUtils.resolvePathUnderDirectory(uploadRootPath, savedFilename);
        if (legacyPath != null && Files.isRegularFile(legacyPath)) {
            return Optional.of(legacyPath);
        }
        return Optional.empty();
    }

    private String buildDownloadFilename(String originalFilename, Path downloadPath) {
        if (originalFilename != null && !originalFilename.isBlank()) {
            return originalFilename;
        }
        Path fileName = downloadPath.getFileName();
        return fileName == null ? "image" : fileName.toString();
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
