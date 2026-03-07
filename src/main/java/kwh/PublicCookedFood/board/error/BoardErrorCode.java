package kwh.PublicCookedFood.board.error;

import kwh.PublicCookedFood.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum BoardErrorCode implements ErrorCode {
    BOARD_VIEW_BLOCKED("BOARD_VIEW_BLOCKED", HttpStatus.FORBIDDEN, "차단 관계인 사용자의 게시글은 조회할 수 없습니다."),
    BOARD_COMMENT_BLOCKED("BOARD_COMMENT_BLOCKED", HttpStatus.FORBIDDEN, "차단 관계인 사용자에게는 댓글을 작성할 수 없습니다."),
    BOARD_LIKE_BLOCKED("BOARD_LIKE_BLOCKED", HttpStatus.FORBIDDEN, "차단 관계인 사용자의 게시글에는 좋아요를 누를 수 없습니다."),
    BOARD_SCRAP_BLOCKED("BOARD_SCRAP_BLOCKED", HttpStatus.FORBIDDEN, "차단 관계인 사용자의 게시글은 스크랩할 수 없습니다."),
    BOARD_REPORT_DUPLICATED("BOARD_REPORT_DUPLICATED", HttpStatus.CONFLICT, "이미 신고한 게시글입니다."),
    BOARD_REPORT_BLOCKED("BOARD_REPORT_BLOCKED", HttpStatus.FORBIDDEN, "차단 관계인 사용자의 게시글은 신고할 수 없습니다."),
    BOARD_REPORT_RATE_LIMITED("BOARD_REPORT_RATE_LIMITED", HttpStatus.TOO_MANY_REQUESTS, "신고 요청이 너무 빠릅니다. 잠시 후 다시 시도해주세요."),
    BOARD_REPORT_DAILY_LIMIT_EXCEEDED("BOARD_REPORT_DAILY_LIMIT_EXCEEDED", HttpStatus.TOO_MANY_REQUESTS, "하루 신고 가능 횟수를 초과했습니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    BoardErrorCode(String code, HttpStatus status, String message) {
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
