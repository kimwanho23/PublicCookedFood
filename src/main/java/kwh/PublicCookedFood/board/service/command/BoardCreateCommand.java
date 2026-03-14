package kwh.PublicCookedFood.board.service.command;

import java.util.Objects;

public final class BoardCreateCommand {

    private final long actorAccountId;
    private final BoardSaveCommand saveCommand;

    public BoardCreateCommand(long actorAccountId, BoardSaveCommand saveCommand) {
        if (actorAccountId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 사용자입니다.");
        }
        this.actorAccountId = actorAccountId;
        this.saveCommand = Objects.requireNonNull(saveCommand, "게시글 생성 명령이 비어 있습니다.");
    }

    public static BoardCreateCommand of(Long actorAccountId, BoardSaveCommand saveCommand) {
        return new BoardCreateCommand(
                Objects.requireNonNull(actorAccountId, "유효하지 않은 사용자입니다."),
                saveCommand
        );
    }

    public long actorAccountId() {
        return actorAccountId;
    }

    public BoardSaveCommand saveCommand() {
        return saveCommand;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BoardCreateCommand)) {
            return false;
        }
        BoardCreateCommand that = (BoardCreateCommand) other;
        return actorAccountId == that.actorAccountId
                && Objects.equals(saveCommand, that.saveCommand);
    }

    @Override
    public int hashCode() {
        return Objects.hash(actorAccountId, saveCommand);
    }
}
