package kwh.PublicCookedFood.user.domain;

import jakarta.persistence.*;
import kwh.PublicCookedFood.food.entity.Recipe_INFO;
import kwh.PublicCookedFood.user.dto.BookMarkDto;
import lombok.Builder;
import lombok.Getter;

@Entity
@Getter
@Table(name = "bookmark")
public class Bookmark {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email", referencedColumnName = "email", nullable = false)
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_ID", referencedColumnName = "row_NUM", nullable = false)
    private Recipe_INFO recipeID;

    public Bookmark() {
    }

    @Builder
    public Bookmark(Long id, Users user, Recipe_INFO recipeID) {
        this.id = id;
        this.user = user;
        this.recipeID = recipeID;
    }

    public BookMarkDto toResponseDto(){
        return BookMarkDto.builder()
                .user(user)
                .recipeID(recipeID)
                .build();
    }


}
