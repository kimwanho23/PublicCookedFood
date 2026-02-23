package kwh.PublicCookedFood.board.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BoardSectionUpdateRequest {

    @NotBlank(message = "게시판 이름은 필수입니다.")
    @Size(max = 100, message = "게시판 이름은 100자 이하여야 합니다.")
    private String sectionName;

    @Min(value = 0, message = "정렬 순서는 0 이상이어야 합니다.")
    private Integer displayOrder;

    private Boolean active;
}
