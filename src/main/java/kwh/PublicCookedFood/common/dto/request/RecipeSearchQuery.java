package kwh.PublicCookedFood.common.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RecipeSearchQuery {

    @Size(max = 50, message = "분류 필터는 50자 이하로 입력해주세요.")
    private String type;

    @Size(max = 50, message = "음식별 필터는 50자 이하로 입력해주세요.")
    private String nation;

    @Size(max = 50, message = "재료별 필터는 50자 이하로 입력해주세요.")
    private String ingredient;

    @Size(max = 50, message = "카테고리 필터는 50자 이하로 입력해주세요.")
    private String keyword;

    @Size(max = 100, message = "검색어는 100자 이하로 입력해주세요.")
    private String search;

    public String normalizedKeyword() {
        return normalize(keyword);
    }

    public String normalizedType() {
        return normalize(type);
    }

    public String normalizedNation() {
        return normalize(nation);
    }

    public String normalizedIngredient() {
        return normalize(ingredient);
    }

    public String normalizedSearch() {
        return normalize(search);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
