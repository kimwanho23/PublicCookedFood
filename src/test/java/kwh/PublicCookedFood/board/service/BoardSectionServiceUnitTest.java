package kwh.PublicCookedFood.board.service;

import kwh.PublicCookedFood.board.domain.BoardSection;
import kwh.PublicCookedFood.board.error.BoardSectionErrorCode;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import kwh.PublicCookedFood.board.repository.BoardSectionRepository;
import kwh.PublicCookedFood.common.error.AppException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardSectionServiceUnitTest {

    @Mock
    private BoardSectionRepository boardSectionRepository;

    @Mock
    private BoardRepository boardRepository;

    @InjectMocks
    private BoardSectionService boardSectionService;

    @Test
    void createSection_throwsAppExceptionWhenSectionKeyIsDuplicated() {
        when(boardSectionRepository.existsBySectionKey("tips")).thenReturn(true);

        assertThatThrownBy(() -> boardSectionService.createSection("tips", "Tips", 1))
                .isInstanceOfSatisfying(AppException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(BoardSectionErrorCode.BOARD_SECTION_KEY_DUPLICATED));
    }

    @Test
    void createSection_generatesSectionKeyAndAppendsToEndWhenKeyAndOrderAreMissing() {
        BoardSection savedSection = BoardSection.builder()
                .id(3L)
                .sectionKey("section")
                .sectionName("###")
                .displayOrder(3)
                .active(true)
                .build();
        when(boardSectionRepository.existsBySectionKey("section")).thenReturn(false);
        when(boardSectionRepository.findFirstByOrderByDisplayOrderDescIdDesc())
                .thenReturn(Optional.of(BoardSection.builder()
                        .id(2L)
                        .sectionKey("tips")
                        .sectionName("Tips")
                        .displayOrder(2)
                        .active(true)
                        .build()));
        when(boardSectionRepository.save(any(BoardSection.class))).thenReturn(savedSection);

        BoardSection result = boardSectionService.createSection(null, "###", null);

        assertThat(result.getSectionKey()).isEqualTo("section");
        assertThat(result.getDisplayOrder()).isEqualTo(3);
        verify(boardSectionRepository).save(any(BoardSection.class));
    }

    @Test
    void reorderSections_updatesDisplayOrderInRequestOrder() {
        BoardSection first = BoardSection.builder()
                .id(10L)
                .sectionKey("tips")
                .sectionName("Tips")
                .displayOrder(5)
                .active(true)
                .build();
        BoardSection second = BoardSection.builder()
                .id(20L)
                .sectionKey("news")
                .sectionName("News")
                .displayOrder(1)
                .active(true)
                .build();
        when(boardSectionRepository.findAllById(any())).thenReturn(List.of(first, second));

        List<BoardSection> reordered = boardSectionService.reorderSections(List.of(20L, 10L));

        assertThat(reordered).extracting(BoardSection::getId).containsExactly(20L, 10L);
        assertThat(reordered).extracting(BoardSection::getDisplayOrder).containsExactly(0, 1);
    }

    @Test
    void deleteSection_movesBoardsToDefaultSectionBeforeDelete() {
        BoardSection section = BoardSection.builder()
                .id(1L)
                .sectionKey("tips")
                .sectionName("Tips")
                .displayOrder(1)
                .active(true)
                .build();
        BoardSection defaultSection = BoardSection.builder()
                .id(2L)
                .sectionKey("general")
                .sectionName("일반")
                .displayOrder(0)
                .active(true)
                .build();
        when(boardSectionRepository.findById(1L)).thenReturn(Optional.of(section));
        when(boardSectionRepository.findBySectionKey("general")).thenReturn(Optional.of(defaultSection));

        boardSectionService.deleteSection(1L);

        verify(boardRepository).reassignSection(section, defaultSection);
        verify(boardSectionRepository).delete(section);
    }

    @Test
    void deleteSection_throwsAppExceptionWhenSectionIsDefault() {
        BoardSection defaultSection = BoardSection.builder()
                .id(1L)
                .sectionKey("general")
                .sectionName("일반")
                .displayOrder(0)
                .active(true)
                .build();
        when(boardSectionRepository.findById(1L)).thenReturn(Optional.of(defaultSection));

        assertThatThrownBy(() -> boardSectionService.deleteSection(1L))
                .isInstanceOfSatisfying(AppException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(BoardSectionErrorCode.BOARD_SECTION_DEFAULT_DELETE_FORBIDDEN));

        verify(boardRepository, never()).reassignSection(any(), any());
        verify(boardSectionRepository, never()).delete(any());
    }
}
