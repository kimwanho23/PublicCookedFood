package kwh.PublicCookedFood.board.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kwh.PublicCookedFood.board.domain.BoardReportReason;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BoardReportCreateRequest {

    @NotNull(message = "신고 사유를 선택해주세요.")
    private BoardReportReason reason;

    @Size(max = 500, message = "신고 상세 내용은 500자 이하로 입력해주세요.")
    private String details;
}
