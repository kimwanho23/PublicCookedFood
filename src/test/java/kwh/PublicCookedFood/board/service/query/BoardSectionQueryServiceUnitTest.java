package kwh.PublicCookedFood.board.service.query;

import kwh.PublicCookedFood.board.domain.BoardSection;
import kwh.PublicCookedFood.board.repository.BoardSectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardSectionQueryServiceUnitTest {

    @Mock
    private BoardSectionRepository boardSectionRepository;

    private BoardSectionQueryService boardSectionQueryService;

    @BeforeEach
    void setUp() {
        boardSectionQueryService = new BoardSectionQueryService(boardSectionRepository);
    }

    @Test
    void getActiveSections_returnsDefaultFallbackWhenRepositoryIsEmpty() {
        when(boardSectionRepository.findAllByActiveTrueOrderByDisplayOrderAscIdAsc()).thenReturn(List.of());

        List<BoardSection> sections = boardSectionQueryService.getActiveSections();

        assertThat(sections).hasSize(1);
        assertThat(sections.get(0).getSectionKey()).isEqualTo("general");
        assertThat(sections.get(0).getId()).isNull();
    }

    @Test
    void existsActiveSection_acceptsDefaultSectionWhenRepositoryIsEmpty() {
        when(boardSectionRepository.findBySectionKeyAndActiveTrue("general")).thenReturn(Optional.empty());

        boolean exists = boardSectionQueryService.existsActiveSection("general");

        assertThat(exists).isTrue();
        verify(boardSectionRepository).findBySectionKeyAndActiveTrue("general");
    }
}
