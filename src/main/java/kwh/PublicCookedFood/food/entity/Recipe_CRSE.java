package kwh.PublicCookedFood.food.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Builder;

@Entity
public class Recipe_CRSE {

    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    private Long row_NUM;

    private Long recipe_ID;     // 레시피 코드

    private String cooking_NO;  // 요리 설명 순서

    private String cooking_DC;	// 요리 설명

    private String step_TIP;	// 과정 팁

    @Builder
    public Recipe_CRSE(Long row_NUM, Long recipe_ID, String cooking_NO, String cooking_DC, String step_TIP) {
        this.row_NUM = row_NUM;
        this.recipe_ID = recipe_ID;
        this.cooking_NO = cooking_NO;
        this.cooking_DC = cooking_DC;
        this.step_TIP = step_TIP;
    }

    public Recipe_CRSE() {

    }
}
