package kwh.PublicCookedFood.board.dto;

import lombok.Getter;

@Getter
public class ImagesDto {

    private Long postId; // 게시글 번호

    private String originalFilename; //원본 파일

    private String savedFilename; // 저장 파일(서버)

    private String imgUrl; // 이미지 경로
}
