package kwh.PublicCookedFood.board.service;

import kwh.PublicCookedFood.board.service.image.BoardImagesZipQuery;
import kwh.PublicCookedFood.board.service.image.BoardImagesZipResource;
import kwh.PublicCookedFood.board.service.image.ZipImageEntry;
import kwh.PublicCookedFood.storage.ImageDownloadQuery;
import kwh.PublicCookedFood.storage.ImageDownloadResource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
public class BoardImageArchiveService {

    private final OriginalImageDownloadService originalImageDownloadService;
    private final ImageUrlSupport imageUrlSupport;

    @Transactional(readOnly = true)
    public Optional<BoardImagesZipResource> findBoardImagesForZip(BoardImagesZipQuery query) {
        if (query.boardId() == null) {
            return Optional.empty();
        }

        Set<String> imageUrls = imageUrlSupport.extractLocalImageUrls(query.htmlContent());
        if (imageUrls.isEmpty()) {
            return Optional.empty();
        }

        List<ZipImageEntry> zipEntries = new ArrayList<>();
        Set<Path> includedPaths = new HashSet<>();
        Map<String, Integer> filenameSequence = new HashMap<>();

        for (String imageUrl : imageUrls) {
            Optional<ImageDownloadResource> downloadResourceOptional =
                    originalImageDownloadService.findOriginalImageForDownload(ImageDownloadQuery.of(imageUrl));
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

        return Optional.of(new BoardImagesZipResource(
                "board-" + query.boardId() + "-images.zip",
                List.copyOf(zipEntries)
        ));
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

        if (candidate.isBlank() || ".".equals(candidate)) {
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
}
