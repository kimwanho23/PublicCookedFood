package kwh.PublicCookedFood.board.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BoardWriteRequest {

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 200, message = "제목은 200자 이하로 입력해주세요.")
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    @Size(max = 10000, message = "내용은 10000자 이하로 입력해주세요.")
    private String contents;

    @NotNull(message = "게시판 탭은 필수입니다.")
    private Long sectionId;

    public static BoardWriteRequest prepared(BoardWriteRequest source, Long fallbackSectionId) {
        BoardWriteRequest prepared = new BoardWriteRequest();
        if (source != null) {
            prepared.setTitle(source.getTitle());
            prepared.setContents(source.getContents());
            prepared.setSectionId(source.getSectionId());
        }
        prepared.setSectionId(resolveSectionId(prepared.getSectionId(), fallbackSectionId));
        return prepared;
    }

    private static Long resolveSectionId(Long sectionId, Long fallbackSectionId) {
        if (sectionId != null) {
            return sectionId;
        }
        return fallbackSectionId;
    }
}
