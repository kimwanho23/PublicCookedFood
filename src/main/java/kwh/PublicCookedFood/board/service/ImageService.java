package kwh.PublicCookedFood.board.service;

import kwh.PublicCookedFood.board.domain.Board;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final ImageTempUploadService imageTempUploadService;
    private final ImageReferenceService imageReferenceService;
    private final ImageDownloadQueryService imageDownloadQueryService;

    public String uploadTempImage(MultipartFile file) {
        return imageTempUploadService.uploadTempImage(file);
    }

    public void syncBoardImages(Board board) {
        if (board == null) {
            return;
        }
        imageReferenceService.syncBoardImages(board, board.getContents());
    }

    public void syncBoardImages(Board board, String htmlContent) {
        imageReferenceService.syncBoardImages(board, htmlContent);
    }

    public void deleteBoardImages(Board board) {
        imageReferenceService.deleteBoardImages(board);
    }

    public int deleteStaleTempImages(LocalDateTime cutoff, int batchSize) {
        return imageReferenceService.deleteStaleTempImages(cutoff, batchSize);
    }

    public Optional<ImageDownloadResource> findOriginalImageForDownload(String imageUrl) {
        return imageDownloadQueryService.findOriginalImageForDownload(imageUrl);
    }

    public Optional<BoardImagesZipResource> findBoardImagesForZip(Long boardId, String htmlContent) {
        return imageDownloadQueryService.findBoardImagesForZip(boardId, htmlContent);
    }

    public void attachProfileImageIfPresent(String imageUrl) {
        imageReferenceService.attachProfileImageIfPresent(imageUrl);
    }

    public void cleanupImageByUrlIfUnlinked(String imageUrl) {
        imageReferenceService.cleanupImageByUrlIfUnlinked(imageUrl);
    }

    public record ImageDownloadResource(Path filePath, String downloadFilename, String contentType, long fileSize) {
    }

    public record BoardImagesZipResource(String zipFilename, List<ZipImageEntry> entries) {
        public BoardImagesZipResource {
            entries = entries == null ? List.of() : List.copyOf(entries);
        }

        @Override
        public List<ZipImageEntry> entries() {
            return List.copyOf(entries);
        }
    }

    public record ZipImageEntry(Path filePath, String entryName) {
    }
}
