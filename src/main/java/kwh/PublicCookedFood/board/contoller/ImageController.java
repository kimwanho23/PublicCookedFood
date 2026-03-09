package kwh.PublicCookedFood.board.contoller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kwh.PublicCookedFood.board.facade.BoardImageFacade;
import kwh.PublicCookedFood.board.service.ImageService;
import kwh.PublicCookedFood.storage.StorageCategory;
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
@Tag(name = "게시판 이미지 API", description = "게시판 이미지의 업로드와 다운로드를 처리하는 API")
public class ImageController {

    private final BoardImageFacade boardImageFacade;
    private final ImageService imageService;

    @Operation(summary = "임시 이미지 업로드", description = "게시글 작성 전에 이미지를 임시 저장하고 접근 URL을 반환합니다.")
    @PostMapping("/api/images")
    public ResponseEntity<String> uploadImage(
            @Parameter(description = "업로드할 이미지 파일", required = true)
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(boardImageFacade.uploadTempImage(file));
    }

    @Operation(summary = "사용자 레시피 이미지 업로드", description = "사용자 레시피 작성/수정에 사용할 이미지를 임시 저장하고 접근 URL을 반환합니다.")
    @PostMapping("/api/images/user-recipes")
    public ResponseEntity<String> uploadUserRecipeImage(
            @Parameter(description = "업로드할 이미지 파일", required = true)
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(imageService.uploadTempImage(file, StorageCategory.USER_RECIPE_IMAGE));
    }

    @Operation(summary = "원본 이미지 다운로드", description = "원본 이미지 URL을 기준으로 이미지를 조회하거나 다운로드합니다.")
    @GetMapping("/api/images/original")
    public ResponseEntity<Resource> downloadOriginalImage(
            @Parameter(description = "조회할 원본 이미지 URL", required = true)
            @RequestParam("url") String imageUrl,
            @Parameter(description = "true이면 첨부파일 다운로드 형식으로 응답합니다.")
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

    @Operation(summary = "게시글 이미지 ZIP 다운로드", description = "게시글에 포함된 이미지를 ZIP 파일로 묶어 다운로드합니다.")
    @GetMapping("/api/images/boards/{boardId}/zip")
    public ResponseEntity<StreamingResponseBody> downloadBoardImagesZip(
            @Parameter(description = "이미지를 압축 다운로드할 게시글 ID", required = true)
            @PathVariable Long boardId
    ) {
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
