package kwh.PublicCookedFood.account.domain;

import jakarta.persistence.*;
import kwh.PublicCookedFood.food.entity.Recipe_INFO;
import lombok.Builder;
import lombok.Getter;

@Entity
@Getter
@Table(name = "bookmark",  uniqueConstraints = {
        @UniqueConstraint(name = "uk_bookmark_account_recipe", columnNames = {"account_id", "recipe_ID"})
})
public class Bookmark {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", referencedColumnName = "id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_ID", referencedColumnName = "row_NUM", nullable = false)
    private Recipe_INFO recipeID;

    public Bookmark() {
    }

    @Builder
    public Bookmark(Long id, Account account, Recipe_INFO recipeID) {
        this.id = id;
        this.account = account;
        this.recipeID = recipeID;
    }

    public static Bookmark of(Account account, Recipe_INFO recipe) {
        return Bookmark.builder()
                .account(account)
                .recipeID(recipe)
                .build();
    }
}
