package kwh.PublicCookedFood.board.service.command;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardSection;

import java.util.Objects;

public final class BoardSaveCommand {

    private final String title;
    private final String contents;
    private final long sectionId;

    public BoardSaveCommand(String title,
                            String contents,
                            long sectionId) {
        if (sectionId <= 0) {
            throw new IllegalArgumentException("게시판 탭이 비어 있습니다.");
        }
        this.title = title;
        this.contents = contents;
        this.sectionId = sectionId;
    }

    public static BoardSaveCommand forCreate(String title,
                                             String contents,
                                             Long sectionId) {
        return new BoardSaveCommand(
                title,
                contents,
                Objects.requireNonNull(sectionId, "게시판 탭이 비어 있습니다.")
        );
    }

    public String title() {
        return title;
    }

    public String contents() {
        return contents;
    }

    public long sectionId() {
        return sectionId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BoardSaveCommand)) {
            return false;
        }
        BoardSaveCommand that = (BoardSaveCommand) other;
        return sectionId == that.sectionId
                && Objects.equals(title, that.title)
                && Objects.equals(contents, that.contents);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, contents, sectionId);
    }

    public Board toBoard(Account account,
                         BoardSection section,
                         String sanitizedTitle,
                         String sanitizedContents) {
        return Board.create(sanitizedTitle, sanitizedContents, account, section);
    }
}
