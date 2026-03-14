package kwh.PublicCookedFood.board.service.image;

import java.util.Objects;

public final class BoardImagesZipQuery {

    private final Long boardId;
    private final String htmlContent;

    public BoardImagesZipQuery(Long boardId, String htmlContent) {
        this.boardId = boardId;
        this.htmlContent = htmlContent;
    }

    public static BoardImagesZipQuery of(Long boardId, String htmlContent) {
        return new BoardImagesZipQuery(boardId, htmlContent);
    }

    public Long boardId() {
        return boardId;
    }

    public String htmlContent() {
        return htmlContent;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BoardImagesZipQuery)) {
            return false;
        }
        BoardImagesZipQuery that = (BoardImagesZipQuery) other;
        return Objects.equals(boardId, that.boardId)
                && Objects.equals(htmlContent, that.htmlContent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(boardId, htmlContent);
    }
}
