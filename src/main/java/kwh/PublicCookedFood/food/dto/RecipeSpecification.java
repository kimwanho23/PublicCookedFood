package kwh.PublicCookedFood.food.dto;

import kwh.PublicCookedFood.food.entity.Recipe_INFO;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class RecipeSpecification {


    public static Specification<Recipe_INFO> hasTyNM(List<String> tyNmList) {
        return (root, query, criteriaBuilder) -> root.get("tyNM").in(tyNmList);
    }

    public static Specification<Recipe_INFO> hasNationNM(List<String> nationNmList) {
        return (root, query, criteriaBuilder) -> root.get("nationNM").in(nationNmList);
    }

    public static Specification<Recipe_INFO> hasIrdntCode(List<String> irdntCodeList) {
        return (root, query, criteriaBuilder) -> root.get("irdntCODE").in(irdntCodeList);
    }
}
