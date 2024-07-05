package kwh.PublicCookedFood.food.dto.recipe_irdnt;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class Recipe_IRDNT_ResponseDto {

    private Long ROW_NUM;           // ID
    private Long RECIPE_ID;         // 레시피 코드
    private String IRDNT_SN;	    // 재료순번
    private String IRDNT_NM;	    // 재료명
    private String IRDNT_CPCTY;	    // 재료용량
    private String IRDNT_TY_CODE;	// 재료타입 코드
    private String IRDNT_TY_NM;	    // 재료타입명
}