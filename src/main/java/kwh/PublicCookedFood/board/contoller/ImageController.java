package kwh.PublicCookedFood.board.contoller;

import io.swagger.v3.oas.annotations.tags.Tag;
import kwh.PublicCookedFood.board.facade.BoardImageFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Tag(name = "Board Image API")
public class ImageController {

    private final BoardImageFacade boardImageFacade;

    @PostMapping("/api/images")
    public ResponseEntity<String> uploadImage(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(boardImageFacade.uploadTempImage(file));
    }

    @GetMapping("/api/images/original")
    public ResponseEntity<Resource> downloadOriginalImage(
            @RequestParam("url") String imageUrl,
            @RequestParam(value = "download", defaultValue = "false") boolean download
    ) {
        Optional<BoardImageFacade.ImageDownloadViewData> viewDataOptional =
                boardImageFacade.prepareOriginalImageDownload(imageUrl, download);
        if (viewDataOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        BoardImageFacade.ImageDownloadViewData viewData = viewDataOptional.get();
        ResponseEntity.BodyBuilder bodyBuilder = ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(viewData.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, viewData.contentDisposition());
        if (viewData.contentLength() >= 0) {
            bodyBuilder.contentLength(viewData.contentLength());
        }

        return bodyBuilder.body(viewData.resource());
    }

    @GetMapping("/api/images/boards/{boardId}/zip")
    public ResponseEntity<StreamingResponseBody> downloadBoardImagesZip(@PathVariable Long boardId) {
        Optional<BoardImageFacade.BoardImagesZipViewData> viewDataOptional =
                boardImageFacade.prepareBoardImagesZip(boardId);
        if (viewDataOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        BoardImageFacade.BoardImagesZipViewData viewData = viewDataOptional.get();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(viewData.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, viewData.contentDisposition())
                .body(viewData.body());
    }
}
