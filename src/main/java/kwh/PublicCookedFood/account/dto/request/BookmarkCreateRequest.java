package kwh.PublicCookedFood.account.dto.request;

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
    private Long accountId;

    @NotNull
    @Positive
    private Long recipeID;

    @Builder
    public BookmarkCreateRequest(Long accountId, Long recipeID) {
        this.accountId = accountId;
        this.recipeID = recipeID;
    }

    public static BookmarkCreateRequest of(Long accountId, Long recipeId) {
        return BookmarkCreateRequest.builder()
                .accountId(accountId)
                .recipeID(recipeId)
                .build();
    }
}

