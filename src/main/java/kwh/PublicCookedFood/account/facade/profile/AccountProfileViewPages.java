package kwh.PublicCookedFood.account.facade.profile;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.Comments;
import org.springframework.data.domain.Page;

public record AccountProfileViewPages(Page<Board> boardPage,
                                   Page<Comments> commentPage,
                                   Page<Board> scrapPage) {

    public static AccountProfileViewPages boards(Page<Board> boardPage) {
        return new AccountProfileViewPages(boardPage, null, null);
    }

    public static AccountProfileViewPages comments(Page<Comments> commentPage) {
        return new AccountProfileViewPages(null, commentPage, null);
    }

    public static AccountProfileViewPages scraps(Page<Board> scrapPage) {
        return new AccountProfileViewPages(null, null, scrapPage);
    }
}
