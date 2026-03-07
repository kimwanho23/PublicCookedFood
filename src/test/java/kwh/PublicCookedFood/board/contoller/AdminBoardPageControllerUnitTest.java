package kwh.PublicCookedFood.board.contoller;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.board.domain.BoardReportStatus;
import kwh.PublicCookedFood.board.facade.BoardAdminFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBoardPageControllerUnitTest {

    @Mock
    private BoardAdminFacade boardAdminFacade;

    @InjectMocks
    private AdminBoardPageController adminBoardPageController;

    @Test
    void boardAdminRoot_redirectsToDashboard() {
        String viewName = adminBoardPageController.boardAdminRoot();

        assertThat(viewName).isEqualTo("redirect:/admin/boards/dashboard");
    }

    @Test
    void updateReportStatus_keepsOneIndexedPageOnRedirect() {
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        when(boardAdminFacade.normalizeReportFilter("resolved")).thenReturn("RESOLVED");

        String viewName = adminBoardPageController.updateReportStatus(
                10L,
                BoardReportStatus.RESOLVED,
                "done",
                "resolved",
                2,
                adminAccount(99L),
                redirectAttributes
        );

        verify(boardAdminFacade).normalizeReportFilter("resolved");
        verify(boardAdminFacade).updateReportStatus(10L, BoardReportStatus.RESOLVED, "done", 99L);
        assertThat(viewName).isEqualTo("redirect:/admin/boards/reports?status=RESOLVED&page=2");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("reportMessage");
    }

    @Test
    void updateReportStatus_clampsPageToFirstPageWhenUnauthenticated() {
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        when(boardAdminFacade.normalizeReportFilter(BoardAdminFacade.REPORT_FILTER_OPEN))
                .thenReturn(BoardAdminFacade.REPORT_FILTER_OPEN);

        String viewName = adminBoardPageController.updateReportStatus(
                10L,
                BoardReportStatus.OPEN,
                null,
                BoardAdminFacade.REPORT_FILTER_OPEN,
                0,
                null,
                redirectAttributes
        );

        verify(boardAdminFacade).normalizeReportFilter(BoardAdminFacade.REPORT_FILTER_OPEN);
        assertThat(viewName).isEqualTo("redirect:/admin/boards/reports?status=OPEN&page=1");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("reportErrorMessage");
    }

    private Account adminAccount(Long accountId) {
        return Account.builder()
                .id(accountId)
                .email("admin@test.com")
                .name("admin")
                .authority(Role.ADMIN)
                .loginMethod("Current")
                .build();
    }
}
