package kwh.PublicCookedFood.user.dto;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import kwh.PublicCookedFood.food.entity.Recipe_INFO;
import kwh.PublicCookedFood.user.domain.Bookmark;
import kwh.PublicCookedFood.user.domain.Users;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class BookMarkDto {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id")
    private Users user;

    @ManyToOne
    @JoinColumn(name="recipe_ID")
    private Recipe_INFO recipeID;

    public BookMarkDto() {
    }

    @Builder
    public BookMarkDto(Users user, Recipe_INFO recipeID) {
        this.user = user;
        this.recipeID = recipeID;
    }

    public Bookmark toEntity() {
        return Bookmark.builder()
                .user(user)
                .recipeID(recipeID)
                .build();
    }
}
