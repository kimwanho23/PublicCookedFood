package kwh.PublicCookedFood.userrecipe.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserRecipeCommentCreateCommandUnitTest {

    @Test
    void of_createsCommandWithRequiredFields() {
        UserRecipeCommentCreateCommand command = UserRecipeCommentCreateCommand.of(1L, 2L, "comment", 3L);

        assertThat(command.accountId()).isEqualTo(1L);
        assertThat(command.recipeId()).isEqualTo(2L);
        assertThat(command.contents()).isEqualTo("comment");
        assertThat(command.parentId()).isEqualTo(3L);
    }

    @Test
    void of_rejectsBlankContents() {
        assertThatThrownBy(() -> UserRecipeCommentCreateCommand.of(1L, 2L, "   ", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("댓글 내용은 필수입니다.");
    }
}
