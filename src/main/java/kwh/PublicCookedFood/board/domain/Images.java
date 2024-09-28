package kwh.PublicCookedFood.board.domain;

import jakarta.persistence.*;
import kwh.PublicCookedFood.common.BaseEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Images extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; //ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", referencedColumnName = "id", nullable = false)
    private Board board; // 게시글 번호

    private String originalFilename; //원본 파일

    private String savedFilename; // 저장 파일(서버)

    private String imgUrl; // 이미지 경로

    public Images(Long id, Board board, String originalFilename, String savedFilename, String imgUrl) {
        this.id = id;
        this.board = board;
        this.originalFilename = originalFilename;
        this.savedFilename = savedFilename;
        this.imgUrl = imgUrl;
    }
}
