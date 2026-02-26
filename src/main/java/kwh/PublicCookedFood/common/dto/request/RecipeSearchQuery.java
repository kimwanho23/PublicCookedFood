package kwh.PublicCookedFood.common.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
public class RecipeSearchQuery {

    private List<@Size(max = 50, message = "분류 필터는 50자 이하로 입력해주세요.") String> type;

    private List<@Size(max = 50, message = "음식별 필터는 50자 이하로 입력해주세요.") String> nation;

    private List<@Size(max = 50, message = "재료별 필터는 50자 이하로 입력해주세요.") String> ingredient;

    @Size(max = 50, message = "카테고리 필터는 50자 이하로 입력해주세요.")
    private String keyword;

    @Size(max = 100, message = "검색어는 100자 이하로 입력해주세요.")
    private String search;

    public String normalizedKeyword() {
        return normalize(keyword);
    }

    public List<String> normalizedType() {
        return normalizeList(type);
    }

    public List<String> normalizedNation() {
        return normalizeList(nation);
    }

    public List<String> normalizedIngredient() {
        return normalizeList(ingredient);
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

    private List<String> normalizeList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
    }
}
