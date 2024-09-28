package kwh.PublicCookedFood.food.dto.recipe_irdnt;

import kwh.PublicCookedFood.food.entity.Recipe_IRDNT;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@Getter
@ToString
public class Recipe_IRDNT_ResponseDto {

    private Long rowNUM;           // ID

    private Long recipeID;         // 레시피 코드

    private String irdntSN;	    // 재료순번

    private String irdntNM;	    // 재료명

    private String irdntCPCTY;	    // 재료용량

    private String irdntTYCODE;	// 재료타입 코드

    private String irdntTYNM;	    // 재료타입명

    @Builder
    public Recipe_IRDNT_ResponseDto(Long rowNUM, Long recipeID, String irdntSN, String irdntNM, String irdntCPCTY, String irdntTYCODE, String irdntTYNM) {
        this.rowNUM = rowNUM;
        this.recipeID = recipeID;
        this.irdntSN = irdntSN;
        this.irdntNM = irdntNM;
        this.irdntCPCTY = irdntCPCTY;
        this.irdntTYCODE = irdntTYCODE;
        this.irdntTYNM = irdntTYNM;
    }

    public Recipe_IRDNT toEntity() {
        return Recipe_IRDNT.builder()
                .rowNUM(rowNUM)
                .recipeID(recipeID)
                .irdntSN(irdntSN)
                .irdntNM(irdntNM)
                .irdntCPCTY(irdntCPCTY)
                .irdntTYCODE(irdntTYCODE)
                .irdntTYNM(irdntTYNM)
                .build();
    }
}