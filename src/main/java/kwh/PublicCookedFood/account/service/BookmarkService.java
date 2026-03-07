package kwh.PublicCookedFood.account.service;

import org.springframework.transaction.annotation.Transactional;
import kwh.PublicCookedFood.food.entity.Recipe_INFO;
import kwh.PublicCookedFood.food.repository.Recipe_INFO_Repository;
import kwh.PublicCookedFood.metrics.reco.RecipeScoreEventPublisher;
import kwh.PublicCookedFood.metrics.view.RecipeStatsMutationService;
import kwh.PublicCookedFood.account.domain.Bookmark;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.dto.request.BookmarkCreateRequest;
import kwh.PublicCookedFood.account.repository.BookmarkRepository;
import kwh.PublicCookedFood.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final AccountRepository accountRepository;
    private final Recipe_INFO_Repository recipeInfoRepository;
    private final BookmarkRepository bookmarkRepository;
    private final RecipeStatsMutationService recipeStatsMutationService;
    private final RecipeScoreEventPublisher recipeScoreEventPublisher;

    public List<Bookmark> findAccountBookmarks(Account account, String search) {
        String normalizedSearch = normalizeQueryText(search);

        return bookmarkRepository.findBookmarksByAccountId(account.getId()).stream()
                .filter(bookmark -> normalizedSearch == null || containsRecipeText(bookmark, normalizedSearch))
                .toList();
    }

    public boolean isBookmarked(Account account, Recipe_INFO recipeId) { // 이미 북마크한 레시피인지 판단
        return bookmarkRepository.existsByAccountAndRecipeID(account, recipeId);
    }

    @Transactional
    public Bookmark save(BookmarkCreateRequest bookmark) {
        Account account = accountRepository.findById(bookmark.getAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        Recipe_INFO recipe = recipeInfoRepository.findByRecipeID(bookmark.getRecipeID())
                .orElseThrow(() -> new IllegalArgumentException("Recipe not found"));
        try {
            return bookmarkRepository.findByAccountAndRecipeID(account, recipe)
                    .map(existing -> existing)
                    .orElseGet(() -> {
                        Bookmark saved = bookmarkRepository.saveAndFlush(Bookmark.of(account, recipe));
                        recipeStatsMutationService.adjustBookmarkCount(recipe.getRecipeID(), 1L);
                        recipeScoreEventPublisher.publishRecalculateRequest(recipe.getRecipeID(), "BOOKMARK_ADDED");
                        return saved;
                    });
        } catch (DataIntegrityViolationException e) {
            return bookmarkRepository.findByAccountAndRecipeID(account, recipe)
                    .orElseThrow(() -> e);
        }
    }

    @Transactional
    public void delete(Account account, Recipe_INFO recipeID){ // 북마크 해제
        long deleted = bookmarkRepository.deleteByAccountAndRecipeID(account, recipeID);
        if (deleted > 0) {
            recipeStatsMutationService.adjustBookmarkCount(recipeID.getRecipeID(), -1L);
            recipeScoreEventPublisher.publishRecalculateRequest(recipeID.getRecipeID(), "BOOKMARK_REMOVED");
        }
    }

    private String normalizeQueryText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean containsRecipeText(Bookmark bookmark, String keyword) {
        if (bookmark == null || bookmark.getRecipeID() == null) {
            return false;
        }
        String normalizedKeyword = keyword.toLowerCase(Locale.ROOT);
        String recipeName = bookmark.getRecipeID().getRecipeNMKO();
        String summary = bookmark.getRecipeID().getSumry();
        return (recipeName != null && recipeName.toLowerCase(Locale.ROOT).contains(normalizedKeyword))
                || (summary != null && summary.toLowerCase(Locale.ROOT).contains(normalizedKeyword));
    }

}

