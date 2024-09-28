package kwh.PublicCookedFood.board.domain;


import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Getter
@NoArgsConstructor
public class Files {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; //파일 Id

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", referencedColumnName = "id", nullable = false)
    private Board board; // 게시글 번호

    private String fileName; //파일명

    @Builder
    public Files(Long id, Board board, String fileName) {
        this.id = id;
        this.board = board;
        this.fileName = fileName;
    }
}
