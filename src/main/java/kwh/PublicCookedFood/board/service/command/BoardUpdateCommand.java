package kwh.PublicCookedFood.board.service.command;

import java.util.Objects;

public final class BoardUpdateCommand {

    private final long boardId;
    private final long actorAccountId;
    private final long version;
    private final String title;
    private final String contents;
    private final long sectionId;

    public BoardUpdateCommand(long boardId,
                              long actorAccountId,
                              long version,
                              String title,
                              String contents,
                              long sectionId) {
        if (boardId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 게시글입니다.");
        }
        if (actorAccountId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 사용자입니다.");
        }
        if (version < 0) {
            throw new IllegalArgumentException("게시글 수정 버전 정보가 유효하지 않습니다.");
        }
        if (sectionId <= 0) {
            throw new IllegalArgumentException("게시판 탭이 비어 있습니다.");
        }
        this.boardId = boardId;
        this.actorAccountId = actorAccountId;
        this.version = version;
        this.title = Objects.requireNonNull(title, "게시글 제목이 비어 있습니다.");
        this.contents = Objects.requireNonNull(contents, "게시글 내용이 비어 있습니다.");
        this.sectionId = sectionId;
    }

    public static BoardUpdateCommand of(Long actorAccountId,
                                        Long boardId,
                                        Long version,
                                        String title,
                                        String contents,
                                        Long sectionId) {
        return new BoardUpdateCommand(
                Objects.requireNonNull(boardId, "유효하지 않은 게시글입니다."),
                Objects.requireNonNull(actorAccountId, "유효하지 않은 사용자입니다."),
                Objects.requireNonNull(version, "게시글 수정 버전 정보가 유효하지 않습니다."),
                title,
                contents,
                Objects.requireNonNull(sectionId, "게시판 탭이 비어 있습니다.")
        );
    }

    public long boardId() {
        return boardId;
    }

    public long actorAccountId() {
        return actorAccountId;
    }

    public long version() {
        return version;
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
        if (!(other instanceof BoardUpdateCommand)) {
            return false;
        }
        BoardUpdateCommand that = (BoardUpdateCommand) other;
        return boardId == that.boardId
                && actorAccountId == that.actorAccountId
                && version == that.version
                && sectionId == that.sectionId
                && Objects.equals(title, that.title)
                && Objects.equals(contents, that.contents);
    }

    @Override
    public int hashCode() {
        return Objects.hash(boardId, actorAccountId, version, title, contents, sectionId);
    }
}
