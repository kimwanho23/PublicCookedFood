package kwh.PublicCookedFood.account.domain;

import kwh.PublicCookedFood.food.entity.Recipe_INFO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BookmarkFactoryUnitTest {

    @Test
    void of_buildsBookmarkDomain() {
        Account account = Account.builder()
                .id(7L)
                .email("account@test.com")
                .name("tester")
                .authority(Role.USER)
                .loginMethod("Current")
                .build();
        Recipe_INFO recipe = Recipe_INFO.builder()
                .rowNUM(10L)
                .recipeID(100L)
                .recipeNMKO("bibimbap")
                .build();

        Bookmark bookmark = Bookmark.of(account, recipe);

        assertThat(bookmark.getAccount()).isSameAs(account);
        assertThat(bookmark.getRecipeID()).isSameAs(recipe);
    }
}
