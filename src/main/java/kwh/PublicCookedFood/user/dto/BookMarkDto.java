package kwh.PublicCookedFood.user.dto;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import kwh.PublicCookedFood.food.entity.Recipe_INFO;
import kwh.PublicCookedFood.user.domain.Bookmark;
import kwh.PublicCookedFood.user.domain.Users;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@NoArgsConstructor
public class BookMarkDto {
    private String email;

    private Long recipeID;

    @Builder
    public BookMarkDto(String email, Long recipeID) {
        this.email = email;
        this.recipeID = recipeID;
    }

    public Bookmark toEntity(Users user, Recipe_INFO recipeID) {
        return Bookmark.builder()
                .user(user)
                .recipeID(recipeID)
                .build();
    }
}
