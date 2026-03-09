package kwh.PublicCookedFood.userrecipe.error;

import kwh.PublicCookedFood.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum UserRecipeErrorCode implements ErrorCode {
    USER_RECIPE_NOT_FOUND("USER_RECIPE_NOT_FOUND", HttpStatus.NOT_FOUND, "사용자 레시피를 찾을 수 없습니다."),
    USER_RECIPE_FORBIDDEN("USER_RECIPE_FORBIDDEN", HttpStatus.FORBIDDEN, "사용자 레시피를 수정할 권한이 없습니다."),
    USER_RECIPE_COMMENT_BLOCKED("USER_RECIPE_COMMENT_BLOCKED", HttpStatus.FORBIDDEN, "차단 관계인 사용자에게는 댓글을 작성할 수 없습니다."),
    USER_RECIPE_REVIEW_BLOCKED("USER_RECIPE_REVIEW_BLOCKED", HttpStatus.FORBIDDEN, "차단 관계인 사용자에게는 리뷰를 작성할 수 없습니다."),
    USER_RECIPE_REVIEW_SELF_FORBIDDEN("USER_RECIPE_REVIEW_SELF_FORBIDDEN", HttpStatus.FORBIDDEN, "내 레시피에는 리뷰를 작성할 수 없습니다."),
    USER_RECIPE_REVIEW_NOT_FOUND("USER_RECIPE_REVIEW_NOT_FOUND", HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    UserRecipeErrorCode(String code, HttpStatus status, String message) {
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
