package kwh.PublicCookedFood.board.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class BoardSectionReorderRequest {

    @NotEmpty(message = "게시판 탭 순서 정보는 비어 있을 수 없습니다.")
    private List<@NotNull(message = "게시판 탭 ID는 비어 있을 수 없습니다.") Long> sectionIds;
}
