package kwh.PublicCookedFood.account.service;

public record BookmarkCreateCommand(Long accountId, Long recipeId) {

    public static BookmarkCreateCommand of(Long accountId, Long recipeId) {
        return new BookmarkCreateCommand(accountId, recipeId);
    }
}
