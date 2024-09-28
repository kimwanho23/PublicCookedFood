package kwh.PublicCookedFood.food.dto.recipe_crse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import kwh.PublicCookedFood.food.entity.Recipe_CRSE;
import lombok.*;


@Getter
@ToString
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Recipe_CRSE_ResponseDto {

    private Long rowNUM;

    private Long recipeID;     // 레시피 코드

    private String cookingNO;  // 요리 설명 순서

    private String cookingDC;	// 요리 설명

    private String stepTIP;	// 과정 팁

    private String imgURL;

    @Builder
    public Recipe_CRSE_ResponseDto(Long rowNUM, Long recipeID, String cookingNO, String cookingDC, String stepTIP, String imgURL) {
        this.rowNUM = rowNUM;
        this.recipeID = recipeID;
        this.cookingNO = cookingNO;
        this.cookingDC = cookingDC;
        this.stepTIP = stepTIP;
        this.imgURL = imgURL;
    }

    public Recipe_CRSE toEntity() {
        return Recipe_CRSE.builder()
                .rowNUM(rowNUM)
                .recipeID(recipeID)
                .cookingNO(cookingNO)
                .cookingDC(cookingDC)
                .stepTIP(stepTIP)
                .imgURL(imgURL)
                .build();
    }

}
