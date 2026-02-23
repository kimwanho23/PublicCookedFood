package kwh.PublicCookedFood.board.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BoardUpdateRequest {

    private Long id;

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 200, message = "제목은 200자 이하로 입력해주세요.")
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    @Size(max = 10000, message = "내용은 10000자 이하로 입력해주세요.")
    private String contents;

    @NotNull(message = "게시판 탭은 필수입니다.")
    private Long sectionId;

    @Builder
    public BoardUpdateRequest(Long id, String title, String contents, Long sectionId) {
        this.id = id;
        this.title = title;
        this.contents = contents;
        this.sectionId = sectionId;
    }
}
