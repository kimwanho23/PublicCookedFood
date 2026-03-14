package kwh.PublicCookedFood.board.application.query.view;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardSection;

import java.util.Objects;

public final class BoardEditFormView {

    private final Long id;
    private final Long authorAccountId;
    private final Long version;
    private final String title;
    private final String contents;
    private final Long sectionId;

    public BoardEditFormView(Long id,
                             Long authorAccountId,
                             Long version,
                             String title,
                             String contents,
                             Long sectionId) {
        this.id = id;
        this.authorAccountId = authorAccountId;
        this.version = version;
        this.title = title;
        this.contents = contents;
        this.sectionId = sectionId;
    }

    public static BoardEditFormView from(Board board) {
        Objects.requireNonNull(board, "board");
        BoardSection section = board.getSection();
        return new BoardEditFormView(
                board.getId(),
                board.getAccount() == null ? null : board.getAccount().getId(),
                board.getVersion(),
                board.getTitle(),
                board.getContents(),
                section == null ? null : section.getId()
        );
    }

    public Long id() {
        return id;
    }

    public Long authorAccountId() {
        return authorAccountId;
    }

    public Long version() {
        return version;
    }

    public String title() {
        return title;
    }

    public String contents() {
        return contents;
    }

    public Long sectionId() {
        return sectionId;
    }
}
