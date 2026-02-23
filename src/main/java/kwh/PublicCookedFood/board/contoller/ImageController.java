package kwh.PublicCookedFood.board.contoller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;


@Slf4j
@RestController
@RequiredArgsConstructor
public class ImageController {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");

    @Value("${file.dir}")
    private String imgPath; //이미지 파일을 저장할 경로

    @PostMapping("/uploadFile")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) throws IllegalStateException{
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("파일이 비어 있습니다.");
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || !originalFileName.contains(".")) {
            return ResponseEntity.badRequest().body("잘못된 파일명입니다.");
        }

        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf(".")).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(fileExtension)) {
            return ResponseEntity.badRequest().body("지원하지 않는 확장자입니다.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body("이미지 파일만 업로드할 수 있습니다.");
        }

        try {
            Path uploadDir = Path.of(imgPath);
            Files.createDirectories(uploadDir);

            String saveFilename = UUID.randomUUID() + fileExtension; // 저장될 파일 이름
            File targetFile = uploadDir.resolve(saveFilename).normalize().toFile();

            // 파일 저장
            file.transferTo(targetFile);

            String fileUrl = "/images/" + saveFilename;

            return ResponseEntity.ok(fileUrl); // 클라이언트에 URL 반환
        } catch (IOException e) {
            log.error("이미지 업로드 실패", e);
            return ResponseEntity.internalServerError().body("이미지 업로드에 실패했습니다.");
        }
    }
}
