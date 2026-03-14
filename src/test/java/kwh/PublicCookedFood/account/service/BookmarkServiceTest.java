package kwh.PublicCookedFood.account.service;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.domain.Bookmark;
import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.account.repository.AccountRepository;
import kwh.PublicCookedFood.account.repository.BookmarkRepository;
import kwh.PublicCookedFood.food.entity.Recipe_INFO;
import kwh.PublicCookedFood.food.repository.Recipe_INFO_Repository;
import kwh.PublicCookedFood.metrics.reco.RecipeScoreEventPublisher;
import kwh.PublicCookedFood.metrics.view.RecipeStatsMutationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookmarkServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private Recipe_INFO_Repository recipeInfoRepository;

    @Mock
    private BookmarkRepository bookmarkRepository;

    @Mock
    private RecipeStatsMutationService recipeStatsMutationService;

    @Mock
    private RecipeScoreEventPublisher recipeScoreEventPublisher;

    @InjectMocks
    private BookmarkService bookmarkService;

    @Test
    void createAccount() {
        Account account = createAccount(1L, "bookmark-list@test.com");
        Bookmark b1 = Bookmark.of(account, createRecipe(1L, "recipe-1"));
        Bookmark b2 = Bookmark.of(account, createRecipe(2L, "recipe-2"));
        Bookmark b3 = Bookmark.of(account, createRecipe(3L, "recipe-3"));
        when(bookmarkRepository.findBookmarksByAccountId(1L)).thenReturn(List.of(b1, b2, b3));

        List<Bookmark> bookmarks = bookmarkService.findAccountBookmarks(account, null);

        assertThat(bookmarks).hasSize(3);
        verify(bookmarkRepository).findBookmarksByAccountId(1L);
    }

    @Test
    void addBookmarkTest() {
        Account account = createAccount(2L, "bookmark-add@test.com");
        Recipe_INFO recipe = createRecipe(5L, "recipe-5");
        BookmarkCreateCommand request = BookmarkCreateCommand.of(2L, 5L);

        when(accountRepository.findById(2L)).thenReturn(Optional.of(account));
        when(recipeInfoRepository.findByRecipeID(5L)).thenReturn(Optional.of(recipe));
        when(bookmarkRepository.findByAccountAndRecipeID(account, recipe)).thenReturn(Optional.empty());
        when(bookmarkRepository.saveAndFlush(any(Bookmark.class))).thenReturn(Bookmark.of(account, recipe));

        Bookmark saved = bookmarkService.save(request);

        assertThat(saved.getAccount()).isSameAs(account);
        assertThat(saved.getRecipeID()).isSameAs(recipe);
        verify(bookmarkRepository).saveAndFlush(any(Bookmark.class));
        verify(recipeStatsMutationService).adjustBookmarkCount(5L, 1L);
        verify(recipeScoreEventPublisher).publishRecalculateRequest(5L, "BOOKMARK_ADDED");
    }

    @Test
    void addBookmarkTest_returnsExistingBookmarkWhenAlreadySaved() {
        Account account = createAccount(3L, "bookmark-existing@test.com");
        Recipe_INFO recipe = createRecipe(7L, "recipe-7");
        Bookmark existing = Bookmark.of(account, recipe);
        BookmarkCreateCommand request = BookmarkCreateCommand.of(3L, 7L);

        when(accountRepository.findById(3L)).thenReturn(Optional.of(account));
        when(recipeInfoRepository.findByRecipeID(7L)).thenReturn(Optional.of(recipe));
        when(bookmarkRepository.findByAccountAndRecipeID(account, recipe)).thenReturn(Optional.of(existing));

        Bookmark saved = bookmarkService.save(request);

        assertThat(saved).isSameAs(existing);
        verify(bookmarkRepository, never()).saveAndFlush(any(Bookmark.class));
        verify(recipeStatsMutationService, never()).adjustBookmarkCount(7L, 1L);
        verify(recipeScoreEventPublisher, never()).publishRecalculateRequest(7L, "BOOKMARK_ADDED");
    }

    @Test
    void deleteBookmarkTest_decrementsBookmarkStatsWhenDeleted() {
        Account account = createAccount(4L, "bookmark-delete@test.com");
        Recipe_INFO recipe = createRecipe(9L, "recipe-9");
        when(bookmarkRepository.deleteByAccountAndRecipeID(account, recipe)).thenReturn(1L);

        bookmarkService.delete(account, recipe);

        verify(bookmarkRepository).deleteByAccountAndRecipeID(account, recipe);
        verify(recipeStatsMutationService).adjustBookmarkCount(9L, -1L);
        verify(recipeScoreEventPublisher).publishRecalculateRequest(9L, "BOOKMARK_REMOVED");
    }

    @Test
    void deleteBookmarkTest_doesNotDecrementWhenNothingDeleted() {
        Account account = createAccount(5L, "bookmark-delete-none@test.com");
        Recipe_INFO recipe = createRecipe(10L, "recipe-10");
        when(bookmarkRepository.deleteByAccountAndRecipeID(account, recipe)).thenReturn(0L);

        bookmarkService.delete(account, recipe);

        verify(bookmarkRepository).deleteByAccountAndRecipeID(account, recipe);
        verify(recipeStatsMutationService, never()).adjustBookmarkCount(10L, -1L);
        verify(recipeScoreEventPublisher, never()).publishRecalculateRequest(10L, "BOOKMARK_REMOVED");
    }

    private Account createAccount(Long id, String email) {
        return Account.builder()
                .id(id)
                .email(email)
                .name("bookmarker")
                .authority(Role.USER)
                .loginMethod("Current")
                .build();
    }

    private Recipe_INFO createRecipe(Long recipeId, String recipeName) {
        return Recipe_INFO.builder()
                .rowNUM(recipeId)
                .recipeID(recipeId)
                .recipeNMKO(recipeName)
                .build();
    }
}
