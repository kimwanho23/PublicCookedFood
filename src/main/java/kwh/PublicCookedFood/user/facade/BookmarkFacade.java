package kwh.PublicCookedFood.user.facade;

import kwh.PublicCookedFood.food.service.RecipeService;
import kwh.PublicCookedFood.user.audit.BookmarkAuditPublisher;
import kwh.PublicCookedFood.user.domain.Bookmark;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.dto.request.BookmarkCreateRequest;
import kwh.PublicCookedFood.user.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookmarkFacade {

    private final BookmarkService bookmarkService;
    private final RecipeService recipeService;
    private final BookmarkAuditPublisher bookmarkAuditPublisher;

    public List<Bookmark> getUserBookmarks(Users user, String search) {
        return bookmarkService.findUserBookmarks(user, search);
    }

    @Transactional
    public boolean addBookmark(Long userId, Long recipeId) {
        try {
            BookmarkCreateRequest bookmarkDto = BookmarkCreateRequest.of(userId, recipeId);
            bookmarkService.save(bookmarkDto);
            bookmarkAuditPublisher.bookmarkAdd(userId, recipeId);
            return true;
        } catch (IllegalArgumentException e) {
            bookmarkAuditPublisher.bookmarkAddFailed(userId, recipeId, e.getMessage());
            return false;
        }
    }

    @Transactional
    public boolean removeBookmark(Users user, Long recipeId) {
        try {
            bookmarkService.delete(user, recipeService.getRecipeEntityByRecipeId(recipeId));
            bookmarkAuditPublisher.bookmarkRemove(user.getId(), recipeId);
            return true;
        } catch (IllegalArgumentException e) {
            bookmarkAuditPublisher.bookmarkRemoveFailed(user.getId(), recipeId, e.getMessage());
            return false;
        }
    }
}
