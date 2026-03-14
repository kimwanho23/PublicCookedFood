package kwh.PublicCookedFood.userrecipe.service;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.account.repository.AccountRepository;
import kwh.PublicCookedFood.account.service.AccountBlockService;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import kwh.PublicCookedFood.storage.ImageLifecycleService;
import kwh.PublicCookedFood.storage.ImageUrls;
import kwh.PublicCookedFood.userrecipe.domain.UserRecipe;
import kwh.PublicCookedFood.userrecipe.domain.UserRecipeReview;
import kwh.PublicCookedFood.userrecipe.repository.UserRecipeRepository;
import kwh.PublicCookedFood.userrecipe.repository.UserRecipeReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRecipeReviewServiceUnitTest {

    @Mock
    private UserRecipeReviewRepository userRecipeReviewRepository;

    @Mock
    private UserRecipeRepository userRecipeRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountBlockService accountBlockService;

    @Mock
    private ImageLifecycleService imageLifecycleService;

    @InjectMocks
    private UserRecipeReviewService userRecipeReviewService;

    @Test
    void upsertReview_updatesExistingReviewImageAndCleansUpPreviousImage() {
        Account recipeOwner = account(10L, "owner");
        Account reviewer = account(20L, "reviewer");
        UserRecipe recipe = recipe(1L, recipeOwner);
        UserRecipeReview existingReview = UserRecipeReview.builder()
                .id(30L)
                .recipe(recipe)
                .account(reviewer)
                .rating(3)
                .contents("old review")
                .imageUrl("/images/user-recipes/old-review.jpg")
                .build();

        when(userRecipeRepository.findByIdAndState(1L, SoftDeleteState.ACTIVE)).thenReturn(Optional.of(recipe));
        when(accountRepository.findById(20L)).thenReturn(Optional.of(reviewer));
        when(recipe.getAccount()).thenReturn(recipeOwner);
        when(accountBlockService.isEitherBlocked(20L, 10L)).thenReturn(false);
        when(userRecipeReviewRepository.findByRecipeAndAccount(recipe, reviewer)).thenReturn(Optional.of(existingReview));

        userRecipeReviewService.upsertReview(
                1L,
                20L,
                5,
                " updated review ",
                " /images/user-recipes/new-review.jpg "
        );

        verify(userRecipeReviewRepository, never()).save(org.mockito.ArgumentMatchers.any(UserRecipeReview.class));
        verify(imageLifecycleService).attachImagesIfPresent(ImageUrls.single("/images/user-recipes/new-review.jpg"));
        verify(imageLifecycleService).cleanupImagesByUrlIfUnlinked(ImageUrls.single("/images/user-recipes/old-review.jpg"));
    }

    @Test
    void deleteReview_cleansUpSavedReviewImage() {
        Account recipeOwner = account(10L, "owner");
        Account reviewer = account(20L, "reviewer");
        UserRecipe recipe = recipe(1L, recipeOwner);
        UserRecipeReview review = UserRecipeReview.builder()
                .id(30L)
                .recipe(recipe)
                .account(reviewer)
                .rating(4)
                .contents("review")
                .imageUrl("/images/user-recipes/review-delete.jpg")
                .build();

        when(userRecipeRepository.findByIdAndState(1L, SoftDeleteState.ACTIVE)).thenReturn(Optional.of(recipe));
        when(accountRepository.findById(20L)).thenReturn(Optional.of(reviewer));
        when(userRecipeReviewRepository.findByRecipeAndAccount(recipe, reviewer)).thenReturn(Optional.of(review));

        userRecipeReviewService.deleteReview(1L, 20L);

        verify(userRecipeReviewRepository).delete(review);
        verify(userRecipeReviewRepository).flush();
        verify(imageLifecycleService).cleanupImagesByUrlIfUnlinked(ImageUrls.single("/images/user-recipes/review-delete.jpg"));
    }

    private Account account(Long id, String name) {
        return Account.builder()
                .id(id)
                .email(name + "@test.com")
                .name(name)
                .authority(Role.USER)
                .loginMethod("Current")
                .build();
    }

    private UserRecipe recipe(Long id, Account account) {
        return mock(UserRecipe.class);
    }
}
