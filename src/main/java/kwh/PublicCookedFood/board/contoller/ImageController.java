package kwh.PublicCookedFood.board.contoller;

import io.swagger.v3.oas.annotations.tags.Tag;
import kwh.PublicCookedFood.board.service.BoardService;
import kwh.PublicCookedFood.board.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequiredArgsConstructor
@Tag(name = "Board Image API")
public class ImageController {

    private final BoardService boardService;
    private final ImageService imageService;

    @PostMapping("/api/images")
    public ResponseEntity<String> uploadImage(@RequestParam("file") MultipartFile file) {
        String fileUrl = imageService.uploadTempImage(file);
        return ResponseEntity.ok(fileUrl);
    }

    @GetMapping("/api/images/original")
    public ResponseEntity<Resource> downloadOriginalImage(
            @RequestParam("url") String imageUrl,
            @RequestParam(value = "download", defaultValue = "false") boolean download
    ) {
        Optional<ImageService.ImageDownloadResource> downloadResourceOptional = imageService.findOriginalImageForDownload(imageUrl);
        if (downloadResourceOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ImageService.ImageDownloadResource downloadResource = downloadResourceOptional.get();
        try {
            Resource resource = new UrlResource(downloadResource.filePath().toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            ContentDisposition disposition = download
                    ? ContentDisposition.attachment().filename(downloadResource.downloadFilename(), StandardCharsets.UTF_8).build()
                    : ContentDisposition.inline().filename(downloadResource.downloadFilename(), StandardCharsets.UTF_8).build();

            ResponseEntity.BodyBuilder bodyBuilder = ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(downloadResource.contentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString());

            if (downloadResource.fileSize() >= 0) {
                bodyBuilder.contentLength(downloadResource.fileSize());
            }

            return bodyBuilder.body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/api/images/boards/{boardId}/zip")
    public ResponseEntity<StreamingResponseBody> downloadBoardImagesZip(@PathVariable Long boardId) {
        String boardContents;
        try {
            boardContents = boardService.getBoardDetail(boardId).getContents();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }

        Optional<ImageService.BoardImagesZipResource> zipResourceOptional = imageService.findBoardImagesForZip(boardId, boardContents);
        if (zipResourceOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ImageService.BoardImagesZipResource zipResource = zipResourceOptional.get();
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(zipResource.zipFilename(), StandardCharsets.UTF_8)
                .build();

        StreamingResponseBody body = outputStream -> {
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
                for (ImageService.ZipImageEntry entry : zipResource.entries()) {
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

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(body);
    }
}
