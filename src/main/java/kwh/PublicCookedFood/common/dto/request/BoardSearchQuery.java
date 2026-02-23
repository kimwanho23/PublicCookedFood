package kwh.PublicCookedFood.common.dto.request;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BoardSearchQuery {

    @Size(max = 100, message = "검색어는 100자 이하로 입력해주세요.")
    private String search;

    @Size(max = 50, message = "게시판 탭 값이 너무 깁니다.")
    @Pattern(regexp = "^[a-z0-9_-]*$", message = "게시판 탭 값이 올바르지 않습니다.")
    private String section;

    public String normalizedSearch() {
        if (search == null) {
            return null;
        }
        String trimmed = search.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public String normalizedSection() {
        if (section == null) {
            return null;
        }
        String trimmed = section.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase();
    }
}
