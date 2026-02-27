package kwh.PublicCookedFood.user.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BookmarkCreateRequest {

    @NotNull
    @Positive
    private Long userId;

    @NotNull
    @Positive
    private Long recipeID;

    @Builder
    public BookmarkCreateRequest(Long userId, Long recipeID) {
        this.userId = userId;
        this.recipeID = recipeID;
    }

    public static BookmarkCreateRequest of(Long userId, Long recipeId) {
        return BookmarkCreateRequest.builder()
                .userId(userId)
                .recipeID(recipeId)
                .build();
    }
}
