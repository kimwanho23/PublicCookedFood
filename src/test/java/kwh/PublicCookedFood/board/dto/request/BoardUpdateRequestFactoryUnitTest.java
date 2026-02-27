package kwh.PublicCookedFood.board.dto.request;

import kwh.PublicCookedFood.board.dto.response.BoardDetailResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BoardUpdateRequestFactoryUnitTest {

    @Test
    void from_usesExistingSectionIdWhenPresent() {
        BoardDetailResponse detail = BoardDetailResponse.builder()
                .id(1L)
                .title("기존 제목")
                .contents("기존 내용")
                .sectionId(99L)
                .build();

        BoardUpdateRequest request = BoardUpdateRequest.from(detail, 55L);

        assertThat(request.getId()).isEqualTo(1L);
        assertThat(request.getTitle()).isEqualTo("기존 제목");
        assertThat(request.getContents()).isEqualTo("기존 내용");
        assertThat(request.getSectionId()).isEqualTo(99L);
    }

    @Test
    void from_usesFallbackSectionIdWhenExistingIsNull() {
        BoardDetailResponse detail = BoardDetailResponse.builder()
                .id(1L)
                .title("기존 제목")
                .contents("기존 내용")
                .sectionId(null)
                .build();

        BoardUpdateRequest request = BoardUpdateRequest.from(detail, 55L);

        assertThat(request.getSectionId()).isEqualTo(55L);
    }
}
