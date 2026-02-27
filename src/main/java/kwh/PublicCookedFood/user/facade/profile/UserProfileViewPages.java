package kwh.PublicCookedFood.user.facade.profile;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.Comments;
import org.springframework.data.domain.Page;

public record UserProfileViewPages(Page<Board> boardPage,
                                   Page<Comments> commentPage,
                                   Page<Board> scrapPage) {

    public static UserProfileViewPages boards(Page<Board> boardPage) {
        return new UserProfileViewPages(boardPage, null, null);
    }

    public static UserProfileViewPages comments(Page<Comments> commentPage) {
        return new UserProfileViewPages(null, commentPage, null);
    }

    public static UserProfileViewPages scraps(Page<Board> scrapPage) {
        return new UserProfileViewPages(null, null, scrapPage);
    }
}
