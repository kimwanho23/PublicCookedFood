package kwh.PublicCookedFood.food.dto.recipe_info;

import lombok.Builder;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Recipe_INFO_ResponseDto {
    private Long ROW_NUM;
    private Long RECIPE_ID;	// 레시피 코드 (SEQ_RECIPE)
    private String RECIPE_NM_KO;	// 레시피 이름(한글)
    private String SUMRY;	// 간략(요약) 소개
    private String NATION_CODE;	// 유형코드
    private String NATION_NM;	// 유형분류
    private String TY_CODE;	// 음식분류코드
    private String TY_NM;	// 음식분류
    private String COOKING_TIME;	// 조리시간
    private String CALORIE;	// 칼로리
    private String QNT;	// 분량
    private String LEVEL_NM;	// 난이도
    private String IRDNT_CODE;	// 재료별 분류명
    private String PC_NM;	// 가격별 분류

    @Builder
    public Recipe_INFO_ResponseDto(Long ROW_NUM, Long RECIPE_ID, String RECIPE_NM_KO, String SUMRY,
                                   String NATION_CODE, String NATION_NM, String TY_CODE, String TY_NM,
                                   String COOKING_TIME, String CALORIE, String QNT, String LEVEL_NM,
                                   String IRDNT_CODE, String PC_NM) {
        this.ROW_NUM = ROW_NUM;
        this.RECIPE_ID = RECIPE_ID;
        this.RECIPE_NM_KO = RECIPE_NM_KO;
        this.SUMRY = SUMRY;
        this.NATION_CODE = NATION_CODE;
        this.NATION_NM = NATION_NM;
        this.TY_CODE = TY_CODE;
        this.TY_NM = TY_NM;
        this.COOKING_TIME = COOKING_TIME;
        this.CALORIE = CALORIE;
        this.QNT = QNT;
        this.LEVEL_NM = LEVEL_NM;
        this.IRDNT_CODE = IRDNT_CODE;
        this.PC_NM = PC_NM;
    }
}
