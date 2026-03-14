package kwh.PublicCookedFood.userrecipe.service;

public record UserRecipeCommentCreateCommand(Long accountId,
                                             Long recipeId,
                                             String contents,
                                             Long parentId) {

    public UserRecipeCommentCreateCommand {
        if (accountId == null || accountId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 사용자입니다.");
        }
        if (recipeId == null || recipeId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 레시피입니다.");
        }
        if (contents == null || contents.isBlank()) {
            throw new IllegalArgumentException("댓글 내용은 필수입니다.");
        }
        if (parentId != null && parentId <= 0) {
            throw new IllegalArgumentException("부모 댓글 정보가 올바르지 않습니다.");
        }
    }

    public static UserRecipeCommentCreateCommand of(Long accountId,
                                                    Long recipeId,
                                                    String contents,
                                                    Long parentId) {
        return new UserRecipeCommentCreateCommand(accountId, recipeId, contents, parentId);
    }
}
