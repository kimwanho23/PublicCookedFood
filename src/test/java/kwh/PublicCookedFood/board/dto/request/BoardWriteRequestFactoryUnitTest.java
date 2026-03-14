package kwh.PublicCookedFood.board.dto.request;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BoardWriteRequestFactoryUnitTest {

    @Test
    void prepared_copiesSourceAndAppliesFallbackSectionId() {
        BoardWriteRequest source = new BoardWriteRequest();
        source.setTitle("제목");
        source.setContents("내용");

        BoardWriteRequest prepared = BoardWriteRequest.prepared(source, 55L);

        assertThat(source.getSectionId()).isNull();
        assertThat(prepared.getTitle()).isEqualTo("제목");
        assertThat(prepared.getContents()).isEqualTo("내용");
        assertThat(prepared.getSectionId()).isEqualTo(55L);
    }

    @Test
    void prepared_preservesExistingSectionId() {
        BoardWriteRequest source = new BoardWriteRequest();
        source.setSectionId(99L);

        BoardWriteRequest prepared = BoardWriteRequest.prepared(source, 55L);

        assertThat(prepared.getSectionId()).isEqualTo(99L);
    }
}
