package kwh.PublicCookedFood.board.facade;

import kwh.PublicCookedFood.board.service.BoardService;
import kwh.PublicCookedFood.board.service.ImageService;
import org.springframework.core.io.PathResource;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class BoardImageFacade {

    private static final String ZIP_CONTENT_TYPE = "application/zip";

    private final BoardService boardService;
    private final ImageService imageService;

    public String uploadTempImage(MultipartFile file) {
        return imageService.uploadTempImage(file);
    }

    public Optional<ImageDownloadViewData> prepareOriginalImageDownload(String imageUrl, boolean download) {
        Optional<ImageService.ImageDownloadResource> downloadResourceOptional =
                imageService.findOriginalImageForDownload(imageUrl);
        if (downloadResourceOptional.isEmpty()) {
            return Optional.empty();
        }

        ImageService.ImageDownloadResource downloadResource = downloadResourceOptional.get();
        Resource resource = new PathResource(downloadResource.filePath());
        if (!resource.exists() || !resource.isReadable()) {
            return Optional.empty();
        }

        ContentDisposition disposition = download
                ? ContentDisposition.attachment().filename(downloadResource.downloadFilename(), StandardCharsets.UTF_8).build()
                : ContentDisposition.inline().filename(downloadResource.downloadFilename(), StandardCharsets.UTF_8).build();

        return Optional.of(new ImageDownloadViewData(
                resource,
                downloadResource.contentType(),
                disposition.toString(),
                downloadResource.fileSize()
        ));
    }

    public Optional<BoardImagesZipViewData> prepareBoardImagesZip(Long boardId) {
        String boardContents;
        try {
            boardContents = boardService.getBoardDetail(boardId).getContents();
        } catch (NoSuchElementException e) {
            return Optional.empty();
        }

        Optional<ImageService.BoardImagesZipResource> zipResourceOptional =
                imageService.findBoardImagesForZip(boardId, boardContents);
        if (zipResourceOptional.isEmpty()) {
            return Optional.empty();
        }

        ImageService.BoardImagesZipResource zipResource = zipResourceOptional.get();
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(zipResource.zipFilename(), StandardCharsets.UTF_8)
                .build();

        return Optional.of(new BoardImagesZipViewData(
                ZIP_CONTENT_TYPE,
                disposition.toString(),
                zipResource.entries()
        ));
    }

    public record ImageDownloadViewData(Resource resource,
                                        String contentType,
                                        String contentDisposition,
                                        long contentLength) {
    }

    public record BoardImagesZipViewData(String contentType,
                                         String contentDisposition,
                                         List<ImageService.ZipImageEntry> entries) {
        public BoardImagesZipViewData {
            entries = entries == null ? List.of() : List.copyOf(entries);
        }

        @Override
        public List<ImageService.ZipImageEntry> entries() {
            return List.copyOf(entries);
        }

        public StreamingResponseBody body() {
            return outputStream -> {
                try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
                    for (ImageService.ZipImageEntry entry : entries) {
                        if (entry == null || entry.filePath() == null || entry.entryName() == null || entry.entryName().isBlank()) {
                            continue;
                        }
                        if (!Files.isRegularFile(entry.filePath())) {
                            continue;
                        }
                        zipOutputStream.putNextEntry(new ZipEntry(entry.entryName()));
                        try (InputStream inputStream = Files.newInputStream(entry.filePath())) {
                            inputStream.transferTo(zipOutputStream);
                        }
                        zipOutputStream.closeEntry();
                    }
                }
            };
        }
    }
}
