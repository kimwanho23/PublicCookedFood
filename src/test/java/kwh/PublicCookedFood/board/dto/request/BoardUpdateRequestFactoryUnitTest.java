package kwh.PublicCookedFood.board.dto.request;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BoardUpdateRequestFactoryUnitTest {

    @Test
    void prepared_copiesSourceAndAppliesFallbackSectionId() {
        BoardUpdateRequest source = new BoardUpdateRequest();
        source.setVersion(4L);
        source.setTitle("수정 제목");
        source.setContents("수정 내용");

        BoardUpdateRequest request = BoardUpdateRequest.prepared(42L, source, 55L);

        assertThat(source.getId()).isNull();
        assertThat(request.getId()).isEqualTo(42L);
        assertThat(request.getVersion()).isEqualTo(4L);
        assertThat(request.getTitle()).isEqualTo("수정 제목");
        assertThat(request.getContents()).isEqualTo("수정 내용");
        assertThat(request.getSectionId()).isEqualTo(55L);
    }

    @Test
    void prepared_keepsPathIdAuthoritativeEvenWhenSourceIdIsPresent() {
        BoardUpdateRequest source = new BoardUpdateRequest();
        source.setId(99L);
        source.setVersion(8L);
        source.setSectionId(77L);

        BoardUpdateRequest request = BoardUpdateRequest.prepared(42L, source, 55L);

        assertThat(request.getId()).isEqualTo(42L);
        assertThat(request.getVersion()).isEqualTo(8L);
        assertThat(request.getSectionId()).isEqualTo(77L);
    }
}
