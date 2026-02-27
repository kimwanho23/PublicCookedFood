package kwh.PublicCookedFood.user.domain;

import kwh.PublicCookedFood.food.entity.Recipe_INFO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BookmarkFactoryUnitTest {

    @Test
    void of_buildsBookmarkDomain() {
        Users user = Users.builder()
                .id(7L)
                .email("user@test.com")
                .name("테스터")
                .authority(Role.USER)
                .loginMethod("Current")
                .build();
        Recipe_INFO recipe = Recipe_INFO.builder()
                .rowNUM(10L)
                .recipeID(100L)
                .recipeNMKO("비빔밥")
                .build();

        Bookmark bookmark = Bookmark.of(user, recipe);

        assertThat(bookmark.getUser()).isSameAs(user);
        assertThat(bookmark.getRecipeID()).isSameAs(recipe);
    }
}
