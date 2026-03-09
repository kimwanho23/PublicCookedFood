package kwh.PublicCookedFood.food.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;

@Table(name = "Recipe_CRSE")
@Entity
@Getter
public class Recipe_CRSE {

    @Id
    @Column(name = "row_NUM")
    private Long rowNUM;

    @Column(name = "recipe_ID")
    private Long recipeID;     // 레시피 코드

    @Column(name = "cooking_NO")
    private String cookingNO;  // 요리 설명 순서

    @Column(name = "cooking_DC")
    private String cookingDC;	// 요리 설명

    @Column(name = "step_TIP")
    private String stepTIP;	// 과정 팁

    @Column(name="img_URL")
    private String imgURL;

    @Builder
    public Recipe_CRSE(Long rowNUM, Long recipeID, String cookingNO, String cookingDC, String stepTIP, String imgURL) {
        this.rowNUM = rowNUM;
        this.recipeID = recipeID;
        this.cookingNO = cookingNO;
        this.cookingDC = cookingDC;
        this.stepTIP = stepTIP;
        this.imgURL = imgURL;
    }

    public Recipe_CRSE() {

    }
}
