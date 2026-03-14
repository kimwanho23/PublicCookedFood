package kwh.PublicCookedFood.board.service.query;

import kwh.PublicCookedFood.board.domain.BoardReportStatus;
import kwh.PublicCookedFood.board.repository.BoardReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardReportQueryServiceUnitTest {

    @Mock
    private BoardReportRepository boardReportRepository;

    private BoardReportQueryService boardReportQueryService;

    @BeforeEach
    void setUp() {
        boardReportQueryService = new BoardReportQueryService(boardReportRepository);
    }

    @Test
    void getReportCountByStatus_usesTotalCountWhenStatusMissing() {
        when(boardReportRepository.count()).thenReturn(6L);

        long count = boardReportQueryService.getReportCountByStatus(null);

        assertThat(count).isEqualTo(6L);
        verify(boardReportRepository).count();
    }

    @Test
    void hasReported_returnsFalseWhenArgumentsMissing() {
        assertThat(boardReportQueryService.hasReported(null, 1L)).isFalse();
        assertThat(boardReportQueryService.hasReported(10L, null)).isFalse();
    }

    @Test
    void getReports_loadsFilteredPageWhenStatusProvided() {
        PageRequest pageable = PageRequest.of(0, 20);
        Page<kwh.PublicCookedFood.board.domain.BoardReport> page = Page.empty(pageable);
        when(boardReportRepository.findByStatusWithBoardAndReporter(BoardReportStatus.OPEN, pageable)).thenReturn(page);

        Page<kwh.PublicCookedFood.board.domain.BoardReport> result = boardReportQueryService.getReports(BoardReportStatus.OPEN, pageable);

        assertThat(result).isSameAs(page);
        verify(boardReportRepository).findByStatusWithBoardAndReporter(BoardReportStatus.OPEN, pageable);
    }
}
