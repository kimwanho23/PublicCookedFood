package kwh.PublicCookedFood.board.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BoardSectionCreateRequest {

    @NotBlank(message = "게시판 키는 필수입니다.")
    @Size(max = 50, message = "게시판 키는 50자 이하여야 합니다.")
    @Pattern(regexp = "^[a-z0-9_-]+$", message = "게시판 키는 소문자/숫자/-/_만 사용할 수 있습니다.")
    private String sectionKey;

    @NotBlank(message = "게시판 이름은 필수입니다.")
    @Size(max = 100, message = "게시판 이름은 100자 이하여야 합니다.")
    private String sectionName;

    @Min(value = 0, message = "정렬 순서는 0 이상이어야 합니다.")
    private Integer displayOrder;
}
