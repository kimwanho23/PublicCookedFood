package kwh.PublicCookedFood.food.dto.recipe_info;

import com.fasterxml.jackson.annotation.JsonProperty;
import kwh.PublicCookedFood.food.entity.Recipe_INFO;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;


@Getter
@NoArgsConstructor
@ToString
public class Recipe_INFO_ResponseDto {

    @JsonProperty("ROW_NUM")
    private Long rowNUM;

    @JsonProperty("RECIPE_ID")
    private Long recipeID; // 레시피 코드 (SEQ_RECIPE)

    @JsonProperty("RECIPE_NM_KO")
    private String recipeNMKO; // 레시피 이름(한글)

    @JsonProperty("SUMRY")
    private String sumry; // 간략(요약) 소개

    @JsonProperty("NATION_CODE")
    private String nationCODE; // 유형코드

    @JsonProperty("NATION_NM")
    private String nationNM; // 유형분류

    @JsonProperty("TY_CODE")
    private String tyCODE; // 음식분류코드

    @JsonProperty("TY_NM")
    private String tyNM; // 음식분류

    @JsonProperty("COOKING_TIME")
    private String cookingTIME; // 조리시간

    @JsonProperty("CALORIE")
    private String calorie; // 칼로리

    @JsonProperty("QNT")
    private String qnt; // 분량

    @JsonProperty("LEVEL_NM")
    private String levelNM; // 난이도

    @JsonProperty("IRDNT_CODE")
    private String irdntCODE; // 재료별 분류명

    @JsonProperty("PC_NM")
    private String pcNM; // 가격별 분류

    @Builder
    public Recipe_INFO_ResponseDto(Long rowNUM, Long recipeID, String recipeNMKO, String sumry, String nationCODE,
                                   String nationNM, String tyCODE, String tyNM, String cookingTIME, String calorie,
                                   String qnt, String levelNM, String irdntCODE, String pcNM) {
        this.rowNUM = rowNUM;
        this.recipeID = recipeID;
        this.recipeNMKO = recipeNMKO;
        this.sumry = sumry;
        this.nationCODE = nationCODE;
        this.nationNM = nationNM;
        this.tyCODE = tyCODE;
        this.tyNM = tyNM;
        this.cookingTIME = cookingTIME;
        this.calorie = calorie;
        this.qnt = qnt;
        this.levelNM = levelNM;
        this.irdntCODE = irdntCODE;
        this.pcNM = pcNM;
    }


    public Recipe_INFO toEntity() {
        return Recipe_INFO.builder()
                .rowNUM(rowNUM)
                .recipeID(recipeID)
                .recipeNMKO(recipeNMKO)
                .sumry(sumry)
                .nationCODE(nationCODE)
                .nationNM(nationNM)
                .tyCODE(tyCODE)
                .tyNM(tyNM)
                .cookingTIME(cookingTIME)
                .calorie(calorie)
                .qnt(qnt)
                .levelNM(levelNM)
                .irdntCODE(irdntCODE)
                .pcNM(pcNM)
                .build();
    }
}

