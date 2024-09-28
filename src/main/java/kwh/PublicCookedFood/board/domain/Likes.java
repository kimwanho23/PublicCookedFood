package kwh.PublicCookedFood.board.domain;

import jakarta.persistence.*;
import kwh.PublicCookedFood.board.dto.LikesDto;
import kwh.PublicCookedFood.user.domain.Users;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "post_id"})
        }
)
public class Likes {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; //Id

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private Users user; //작성자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", referencedColumnName = "id", nullable = false)
    private Board board; //게시글 번호

    @Builder
    public Likes(Long id, Users user, Board board) {
        this.id = id;
        this.user = user;
        this.board = board;
    }

    public LikesDto toResponseDto(){
        return LikesDto.builder()
                .postId(board)
                .userId(user)
                .build();
    }
}
