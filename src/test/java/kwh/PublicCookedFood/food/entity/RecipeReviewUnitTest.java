package kwh.PublicCookedFood.food.entity;

import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.account.domain.Account;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecipeReviewUnitTest {

    @Test
    void builder_throwsWhenRatingIsOutOfRange() {
        assertThatThrownBy(() -> RecipeReview.builder()
                .account(account())
                .recipe(recipe())
                .rating(999)
                .contents("great")
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Account account() {
        return Account.builder()
                .id(1L)
                .email("review@test.com")
                .name("reviewer")
                .authority(Role.USER)
                .loginMethod("Current")
                .build();
    }

    private Recipe_INFO recipe() {
        return Recipe_INFO.builder()
                .recipeID(10L)
                .recipeNMKO("recipe")
                .build();
    }
}
