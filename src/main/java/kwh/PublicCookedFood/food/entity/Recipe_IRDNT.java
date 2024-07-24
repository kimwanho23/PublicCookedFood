package kwh.PublicCookedFood.food.entity;

import jakarta.persistence.*;
import kwh.PublicCookedFood.food.dto.recipe_irdnt.Recipe_IRDNT_ResponseDto;
import lombok.Builder;
import lombok.Getter;

@Entity
@Getter
@Table(name = "Recipe_IRDNT")
public class Recipe_IRDNT {

    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "row_NUM")
    private Long rowNUM;           // ID

    @Column(name = "recipe_ID")
    private Long recipeID;         // 레시피 코드

    @Column(name = "irdnt_SN")
    private String irdntSN;	    // 재료순번

    @Column(name = "irdnt_NM")
    private String irdntNM;	    // 재료명

    @Column(name = "irdnt_CPCTY")
    private String irdntCPCTY;	    // 재료용량

    @Column(name = "irdnt_TY_CODE")
    private String irdntTYCODE;	// 재료타입 코드

    @Column(name = "irdnt_TY_NM")
    private String irdntTYNM;	    // 재료타입명

    public Recipe_IRDNT() {
    }

    @Builder
    public Recipe_IRDNT(Long rowNUM, Long recipeID, String irdntSN, String irdntNM, String irdntCPCTY, String irdntTYCODE, String irdntTYNM) {
        this.rowNUM = rowNUM;
        this.recipeID = recipeID;
        this.irdntSN = irdntSN;
        this.irdntNM = irdntNM;
        this.irdntCPCTY = irdntCPCTY;
        this.irdntTYCODE = irdntTYCODE;
        this.irdntTYNM = irdntTYNM;
    }

    public Recipe_IRDNT_ResponseDto toResponseDto(){
        return Recipe_IRDNT_ResponseDto.builder()
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