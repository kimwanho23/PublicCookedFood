package kwh.PublicCookedFood.board.error;

import kwh.PublicCookedFood.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum BoardSectionErrorCode implements ErrorCode {
    BOARD_SECTION_KEY_REQUIRED("BOARD_SECTION_KEY_REQUIRED", HttpStatus.BAD_REQUEST, "게시판 키는 비어 있을 수 없습니다."),
    BOARD_SECTION_KEY_DUPLICATED("BOARD_SECTION_KEY_DUPLICATED", HttpStatus.CONFLICT, "이미 존재하는 게시판 키입니다."),
    BOARD_SECTION_NOT_FOUND("BOARD_SECTION_NOT_FOUND", HttpStatus.NOT_FOUND, "게시판 탭을 찾을 수 없습니다."),
    BOARD_SECTION_REORDER_INVALID("BOARD_SECTION_REORDER_INVALID", HttpStatus.BAD_REQUEST, "게시판 탭 정렬 요청이 올바르지 않습니다."),
    BOARD_SECTION_DEFAULT_DEACTIVATE_FORBIDDEN("BOARD_SECTION_DEFAULT_DEACTIVATE_FORBIDDEN", HttpStatus.CONFLICT, "기본 게시판 탭은 비활성화할 수 없습니다."),
    BOARD_SECTION_DEFAULT_DELETE_FORBIDDEN("BOARD_SECTION_DEFAULT_DELETE_FORBIDDEN", HttpStatus.CONFLICT, "기본 게시판 탭은 삭제할 수 없습니다."),
    BOARD_SECTION_DELETE_NOT_EMPTY("BOARD_SECTION_DELETE_NOT_EMPTY", HttpStatus.CONFLICT, "게시글이 남아 있는 탭은 삭제할 수 없습니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    BoardSectionErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public String message() {
        return message;
    }
}
