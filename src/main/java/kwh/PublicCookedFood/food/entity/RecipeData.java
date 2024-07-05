package kwh.PublicCookedFood.food.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class RecipeData {

    @Id
    private Integer RECIPE_ID;	//레시피 코드 (SEQ_RECIPE)

    private String RECIPE_NM_KO;	//레시피 이름(한글)
    private String SUMRY; //간략(요약) 소개
    private String NATION_CODE;	//유형코드
    private String NATION_NM;	//유형분류
    private String TY_CODE;	//음식분류코드
    private String TY_NM;	//음식분류
    private String COOKING_TIME;	//조리시간
    private String CALORIE;	//칼로리
    private String QNT;	//분량
    private String LEVEL_NM;	//난이도
    private String IRDNT_CODE;	//재료별
    private String PC_NM;  //분류명

}
