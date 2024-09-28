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
import java.util.*;


@Slf4j
@RestController
@RequiredArgsConstructor
public class ImageController {

    @Value("${file.dir}")
    private String imgPath; //이미지 파일을 저장할 경로

    @PostMapping("/uploadFile")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) throws IllegalStateException{
        try {
            String originalFileName = file.getOriginalFilename(); //원본 파일명
            String fileExtension = originalFileName.substring(originalFileName.lastIndexOf(".")); //확장자

            String saveFilename = UUID.randomUUID() + fileExtension; // 저장될 파일 이름

            // 파일 저장
            file.transferTo(new File(imgPath, saveFilename));

            String fileUrl = "/images/" + saveFilename;

            return ResponseEntity.ok(fileUrl); // 클라이언트에 URL 반환
        } catch (IOException e) {
            e.printStackTrace(); // 에러 로그 출력
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
