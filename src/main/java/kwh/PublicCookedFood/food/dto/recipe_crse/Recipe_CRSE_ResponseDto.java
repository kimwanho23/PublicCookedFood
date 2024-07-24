package kwh.PublicCookedFood.food.dto.recipe_crse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import kwh.PublicCookedFood.food.entity.Recipe_CRSE;
import lombok.*;


@Getter
@ToString
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Recipe_CRSE_ResponseDto {

    @JsonProperty("ROW_NUM")
    private Long rowNUM;

    @JsonProperty("RECIPE_ID")
    private Long recipeID;     // 레시피 코드

    @JsonProperty("COOKING_NO")
    private String cookingNO;  // 요리 설명 순서

    @JsonProperty("COOKING_DC")
    private String cookingDC;	// 요리 설명

    @JsonProperty("STEP_TIP")
    private String stepTIP;	// 과정 팁

    @Builder
    public Recipe_CRSE_ResponseDto(Long rowNUM, Long recipeID, String cookingNO, String cookingDC, String stepTIP) {
        this.rowNUM = rowNUM;
        this.recipeID = recipeID;
        this.cookingNO = cookingNO;
        this.cookingDC = cookingDC;
        this.stepTIP = stepTIP;
    }

    public Recipe_CRSE toEntity() {
        return Recipe_CRSE.builder()
                .rowNUM(rowNUM)
                .recipeID(recipeID)
                .cookingNO(cookingNO)
                .cookingDC(cookingDC)
                .stepTIP(stepTIP)
                .build();
    }

}
