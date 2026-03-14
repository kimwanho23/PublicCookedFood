package kwh.PublicCookedFood.board.domain;

import jakarta.persistence.*;
import kwh.PublicCookedFood.common.BaseTimeEntity;
import kwh.PublicCookedFood.storage.Images;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "board_image",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_board_image_board_id_image_id", columnNames = {"board_id", "image_id"})
        },
        indexes = {
                @Index(name = "idx_board_image_board_id", columnList = "board_id"),
                @Index(name = "idx_board_image_image_id", columnList = "image_id")
        })
public class BoardImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", referencedColumnName = "id", nullable = false)
    private Board board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id", referencedColumnName = "id", nullable = false)
    private Images image;

    @Builder
    public BoardImage(Long id, Board board, Images image) {
        this.id = id;
        this.board = board;
        this.image = image;
    }

    public static BoardImage of(Board board, Images image) {
        return BoardImage.builder()
                .board(board)
                .image(image)
                .build();
    }
}
