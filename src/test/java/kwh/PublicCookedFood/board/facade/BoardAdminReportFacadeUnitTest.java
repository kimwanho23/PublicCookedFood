package kwh.PublicCookedFood.board.facade;

import kwh.PublicCookedFood.account.audit.BoardAuditPublisher;
import kwh.PublicCookedFood.board.domain.BoardReportStatus;
import kwh.PublicCookedFood.board.service.command.BoardReportCommandService;
import kwh.PublicCookedFood.board.service.query.BoardReportQueryService;
import kwh.PublicCookedFood.board.service.query.BoardReportFilter;
import kwh.PublicCookedFood.board.service.query.BoardReportPageQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardAdminReportFacadeUnitTest {

    @Mock
    private BoardReportQueryService boardReportQueryService;

    @Mock
    private BoardReportCommandService boardReportCommandService;

    @Mock
    private BoardAuditPublisher boardAuditPublisher;

    private BoardAdminReportFacade boardAdminReportFacade;

    @BeforeEach
    void setUp() {
        boardAdminReportFacade = new BoardAdminReportFacade(
                boardReportQueryService,
                boardReportCommandService,
                boardAuditPublisher
        );
    }

    @Test
    void loadReportPage_defaultsInvalidFilterToOpen() {
        PageRequest pageable = PageRequest.of(0, 20);
        when(boardReportQueryService.getReports(BoardReportStatus.OPEN, pageable)).thenReturn(Page.empty(pageable));
        when(boardReportQueryService.getReportCountByStatus(BoardReportStatus.OPEN)).thenReturn(1L);
        when(boardReportQueryService.getReportCountByStatus(BoardReportStatus.RESOLVED)).thenReturn(2L);
        when(boardReportQueryService.getReportCountByStatus(BoardReportStatus.REJECTED)).thenReturn(3L);
        when(boardReportQueryService.getReportCountByStatus(null)).thenReturn(6L);

        BoardAdminReportFacade.ReportPageViewData result =
                boardAdminReportFacade.loadReportPage(BoardReportPageQuery.of("invalid", pageable));

        assertThat(result.reportFilter()).isEqualTo(BoardReportFilter.OPEN.paramValue());
        assertThat(result.reportPage().getPageable()).isEqualTo(pageable);
    }
}
