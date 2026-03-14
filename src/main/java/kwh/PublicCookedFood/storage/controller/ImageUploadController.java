package kwh.PublicCookedFood.storage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kwh.PublicCookedFood.storage.ImageStorageService;
import kwh.PublicCookedFood.storage.ImageUploadCommand;
import kwh.PublicCookedFood.storage.StorageCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@Tag(name = "이미지 업로드 API", description = "게시글과 사용자 레시피 작성에 사용할 임시 이미지를 업로드합니다.")
public class ImageUploadController {

    private final ImageStorageService imageStorageService;

    @Operation(summary = "게시글 임시 이미지 업로드", description = "게시글 작성 전에 이미지를 임시 저장하고 접근 URL을 반환합니다.")
    @PostMapping("/api/images")
    public ResponseEntity<String> uploadBoardImage(
            @Parameter(description = "업로드할 이미지 파일", required = true)
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(imageStorageService.uploadTempImage(ImageUploadCommand.of(file)));
    }

    @Operation(summary = "사용자 레시피 이미지 업로드", description = "사용자 레시피 작성/수정에 사용할 이미지를 임시 저장하고 접근 URL을 반환합니다.")
    @PostMapping("/api/images/user-recipes")
    public ResponseEntity<String> uploadUserRecipeImage(
            @Parameter(description = "업로드할 이미지 파일", required = true)
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(imageStorageService.uploadTempImage(
                ImageUploadCommand.of(file, StorageCategory.USER_RECIPE_IMAGE)
        ));
    }
}
