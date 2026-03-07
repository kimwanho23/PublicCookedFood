package kwh.PublicCookedFood.account.facade;

import kwh.PublicCookedFood.common.error.ErrorMessageResolver;
import kwh.PublicCookedFood.food.service.RecipeService;
import kwh.PublicCookedFood.account.audit.BookmarkAuditPublisher;
import kwh.PublicCookedFood.account.domain.Bookmark;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.dto.request.BookmarkCreateRequest;
import kwh.PublicCookedFood.account.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class BookmarkFacade {

    private final BookmarkService bookmarkService;
    private final RecipeService recipeService;
    private final BookmarkAuditPublisher bookmarkAuditPublisher;

    public List<Bookmark> getAccountBookmarks(Account account, String search) {
        return bookmarkService.findAccountBookmarks(account, search);
    }

    @Transactional
    public boolean addBookmark(Long accountId, Long recipeId) {
        return executeBookmarkChange(
                () -> bookmarkService.save(BookmarkCreateRequest.of(accountId, recipeId)),
                () -> bookmarkAuditPublisher.bookmarkAdd(accountId, recipeId),
                message -> bookmarkAuditPublisher.bookmarkAddFailed(accountId, recipeId, message),
                "북마크 추가 처리에 실패했습니다."
        );
    }

    @Transactional
    public boolean removeBookmark(Account account, Long recipeId) {
        return executeBookmarkChange(
                () -> bookmarkService.delete(account, recipeService.getRecipeEntityByRecipeId(recipeId)),
                () -> bookmarkAuditPublisher.bookmarkRemove(account.getId(), recipeId),
                message -> bookmarkAuditPublisher.bookmarkRemoveFailed(account.getId(), recipeId, message),
                "북마크 해제 처리에 실패했습니다."
        );
    }

    private boolean executeBookmarkChange(Runnable action,
                                          Runnable successAudit,
                                          Consumer<String> failureAudit,
                                          String fallbackMessage) {
        try {
            action.run();
            successAudit.run();
            return true;
        } catch (IllegalArgumentException e) {
            failureAudit.accept(ErrorMessageResolver.resolve(e, fallbackMessage));
            return false;
        }
    }
}

