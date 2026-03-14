package kwh.PublicCookedFood.board.facade;

import kwh.PublicCookedFood.board.service.BoardImageService;
import kwh.PublicCookedFood.board.service.image.BoardImagesZipQuery;
import kwh.PublicCookedFood.board.service.image.BoardImagesZipResource;
import kwh.PublicCookedFood.board.service.image.ZipImageEntry;
import kwh.PublicCookedFood.board.service.query.BoardDetailQueryService;
import kwh.PublicCookedFood.storage.ImageDownloadQuery;
import kwh.PublicCookedFood.storage.ImageDownloadResource;
import kwh.PublicCookedFood.storage.ImageStorageService;
import kwh.PublicCookedFood.storage.ImageUploadCommand;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class BoardImageFacade {

    private static final String ZIP_CONTENT_TYPE = "application/zip";

    private final BoardDetailQueryService boardDetailQueryService;
    private final ImageStorageService imageStorageService;
    private final BoardImageService boardImageService;

    public String uploadTempImage(MultipartFile file) {
        return imageStorageService.uploadTempImage(ImageUploadCommand.of(file));
    }

    public Optional<ImageDownloadViewData> prepareOriginalImageDownload(String imageUrl, boolean download) {
        Optional<ImageDownloadResource> downloadResourceOptional =
                imageStorageService.findOriginalImageForDownload(ImageDownloadQuery.of(imageUrl));
        if (downloadResourceOptional.isEmpty()) {
            return Optional.empty();
        }

        ImageDownloadResource downloadResource = downloadResourceOptional.get();
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
            boardContents = boardDetailQueryService.getBoardDetail(boardId).getContents();
        } catch (NoSuchElementException e) {
            return Optional.empty();
        }

        Optional<BoardImagesZipResource> zipResourceOptional =
                boardImageService.findBoardImagesForZip(BoardImagesZipQuery.of(boardId, boardContents));
        if (zipResourceOptional.isEmpty()) {
            return Optional.empty();
        }

        BoardImagesZipResource zipResource = zipResourceOptional.get();
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(zipResource.zipFilename(), StandardCharsets.UTF_8)
                .build();

        return Optional.of(new BoardImagesZipViewData(
                ZIP_CONTENT_TYPE,
                disposition.toString(),
                zipResource.entries()
        ));
    }

    public static final class ImageDownloadViewData {

        private final Resource resource;
        private final String contentType;
        private final String contentDisposition;
        private final long contentLength;

        public ImageDownloadViewData(Resource resource,
                                     String contentType,
                                     String contentDisposition,
                                     long contentLength) {
            this.resource = Objects.requireNonNull(resource, "resource");
            this.contentType = contentType;
            this.contentDisposition = contentDisposition;
            this.contentLength = contentLength;
        }

        public Resource resource() {
            return resource;
        }

        public Resource getResource() {
            return resource;
        }

        public String contentType() {
            return contentType;
        }

        public String getContentType() {
            return contentType;
        }

        public String contentDisposition() {
            return contentDisposition;
        }

        public String getContentDisposition() {
            return contentDisposition;
        }

        public long contentLength() {
            return contentLength;
        }

        public long getContentLength() {
            return contentLength;
        }
    }

    public static final class BoardImagesZipViewData {

        private final String contentType;
        private final String contentDisposition;
        private final List<ZipImageEntry> entries;

        public BoardImagesZipViewData(String contentType,
                                      String contentDisposition,
                                      List<ZipImageEntry> entries) {
            this.contentType = contentType;
            this.contentDisposition = contentDisposition;
            this.entries = entries == null
                    ? Collections.<ZipImageEntry>emptyList()
                    : Collections.unmodifiableList(new ArrayList<ZipImageEntry>(entries));
        }

        public String contentType() {
            return contentType;
        }

        public String getContentType() {
            return contentType;
        }

        public String contentDisposition() {
            return contentDisposition;
        }

        public String getContentDisposition() {
            return contentDisposition;
        }

        public List<ZipImageEntry> entries() {
            return entries;
        }

        public List<ZipImageEntry> getEntries() {
            return entries;
        }

        public StreamingResponseBody body() {
            return outputStream -> {
                try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
                    for (ZipImageEntry entry : entries) {
                        if (entry == null || entry.filePath() == null || entry.entryName() == null || entry.entryName().trim().isEmpty()) {
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
