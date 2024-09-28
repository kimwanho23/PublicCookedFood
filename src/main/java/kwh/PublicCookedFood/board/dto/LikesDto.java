package kwh.PublicCookedFood.board.dto;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.Likes;
import kwh.PublicCookedFood.user.domain.Users;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LikesDto {

    private Users userId; //작성자

    private Board postId; //게시글 번호

    @Builder
    public LikesDto(Users userId, Board postId) {
        this.userId = userId;
        this.postId = postId;
    }

    public Likes toEntity(){
        return Likes.builder()
                .user(userId)
                .board(postId)
                .build();
    }
}
