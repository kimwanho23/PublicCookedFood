package kwh.PublicCookedFood.food.dto.recipe_crse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;


@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class Recipe_CRSE {

    @JsonProperty("ROW_NUM")
    private Long row_NUM;

    @JsonProperty("RECIPE_ID")
    private Long recipe_ID;     // 레시피 코드

    @JsonProperty("COOKING_NO")
    private String cooking_NO;  // 요리 설명 순서

    @JsonProperty("COOKING_DC")
    private String cooking_DC;	// 요리 설명

    @JsonProperty("STEP_TIP")
    private String step_TIP;	// 과정 팁

    public Recipe_CRSE() {
    }

    @Builder
    public Recipe_CRSE(Long row_NUM, Long recipe_ID, String cooking_NO, String cooking_DC, String step_TIP) {
        this.row_NUM = row_NUM;
        this.recipe_ID = recipe_ID;
        this.cooking_NO = cooking_NO;
        this.cooking_DC = cooking_DC;
        this.step_TIP = step_TIP;
    }

    public kwh.PublicCookedFood.food.entity.Recipe_CRSE toEntity() {
        return kwh.PublicCookedFood.food.entity.Recipe_CRSE.builder()
                .row_NUM(row_NUM)
                .recipe_ID(recipe_ID)
                .cooking_NO(cooking_NO)
                .cooking_DC(cooking_DC)
                .step_TIP(step_TIP)
                .build();
    }

}
