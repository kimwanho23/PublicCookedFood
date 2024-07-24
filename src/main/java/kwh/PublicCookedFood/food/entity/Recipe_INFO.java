package kwh.PublicCookedFood.food.entity;

import jakarta.persistence.*;
import kwh.PublicCookedFood.food.dto.recipe_info.Recipe_INFO_ResponseDto;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Entity
@Getter
@Table(name = "Recipe_INFO")
@ToString
public class Recipe_INFO {

    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "row_NUM")
    private Long rowNUM;

    @Column(name = "recipe_ID")
    private Long recipeID;	// 레시피 코드 (SEQ_RECIPE)

    @Column(name = "recipe_NM_KO")
    private String recipeNMKO;	// 레시피 이름(한글)

    @Column(name = "sumry")
    private String sumry;	// 간략(요약) 소개

    @Column(name = "nation_CODE")
    private String nationCODE;	// 유형코드

    @Column(name = "nation_NM")
    private String nationNM;	// 유형분류

    @Column(name = "ty_CODE")
    private String tyCODE;	// 음식분류코드

    @Column(name = "ty_NM")
    private String tyNM;	// 음식분류

    @Column(name = "cooking_TIME")
    private String cookingTIME;	// 조리시간

    @Column(name = "calorie")
    private String calorie;	// 칼로리

    @Column(name = "qnt")
    private String qnt;	// 분량

    @Column(name = "level_NM")
    private String levelNM;	// 난이도

    @Column(name = "irdnt_CODE")
    private String irdntCODE;	// 재료별 분류명

    @Column(name = "pc_NM")
    private String pcNM;	// 가격별 분류

    @Builder
    public Recipe_INFO(Long rowNUM, Long recipeID, String recipeNMKO, String sumry,
                       String nationCODE, String nationNM, String tyCODE, String tyNM,
                       String cookingTIME, String calorie, String qnt, String levelNM,
                       String irdntCODE, String pcNM) {
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

    public Recipe_INFO() {

    }

    public Recipe_INFO_ResponseDto toResponseDto(){
        return Recipe_INFO_ResponseDto.builder()
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
