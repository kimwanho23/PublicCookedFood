package kwh.PublicCookedFood.food.service;

import kwh.PublicCookedFood.account.repository.AccountRepository;
import kwh.PublicCookedFood.food.repository.RecipeReviewRepository;
import kwh.PublicCookedFood.food.repository.Recipe_INFO_Repository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class RecipeReviewServiceUnitTest {

    @Mock
    private RecipeReviewRepository recipeReviewRepository;

    @Mock
    private Recipe_INFO_Repository recipeInfoRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private RecipeReviewService recipeReviewService;

    @Test
    void upsertReview_throwsWhenRatingIsOutOfRange() {
        assertThatThrownBy(() -> recipeReviewService.upsertReview(10L, 1L, 0, "bad"))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(recipeInfoRepository, accountRepository, recipeReviewRepository);
    }

    @Test
    void upsertReview_throwsWhenRatingIsMissing() {
        assertThatThrownBy(() -> recipeReviewService.upsertReview(10L, 1L, null, "bad"))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(recipeInfoRepository, accountRepository, recipeReviewRepository);
    }
}
