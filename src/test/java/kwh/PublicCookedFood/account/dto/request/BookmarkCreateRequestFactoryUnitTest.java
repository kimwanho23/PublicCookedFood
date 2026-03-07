package kwh.PublicCookedFood.account.dto.request;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BookmarkCreateRequestFactoryUnitTest {

    @Test
    void of_buildsBookmarkCreateRequest() {
        BookmarkCreateRequest request = BookmarkCreateRequest.of(1L, 100L);

        assertThat(request.getAccountId()).isEqualTo(1L);
        assertThat(request.getRecipeID()).isEqualTo(100L);
    }
}

