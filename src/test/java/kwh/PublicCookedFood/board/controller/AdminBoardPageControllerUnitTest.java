package kwh.PublicCookedFood.board.controller;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.board.application.query.view.BoardCardView;
import kwh.PublicCookedFood.board.domain.BoardReportStatus;
import kwh.PublicCookedFood.board.facade.BoardAdminDashboardFacade;
import kwh.PublicCookedFood.board.facade.BoardAdminPolicyFacade;
import kwh.PublicCookedFood.board.facade.BoardAdminReportFacade;
import kwh.PublicCookedFood.board.service.command.BoardReportStatusUpdateCommand;
import kwh.PublicCookedFood.board.service.query.BoardReportFilter;
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
    private BoardAdminDashboardFacade boardAdminDashboardFacade;

    @Mock
    private BoardAdminPolicyFacade boardAdminPolicyFacade;

    @Mock
    private BoardAdminReportFacade boardAdminReportFacade;

    @InjectMocks
    private AdminBoardPageController adminBoardPageController;

    @Test
    void boardAdminRoot_redirectsToDashboard() {
        String viewName = adminBoardPageController.boardAdminRoot();

        assertThat(viewName).isEqualTo("redirect:/admin/boards/dashboard");
    }

    @Test
    void boardDashboardPage_addsPopularBoardsToModel() {
        org.springframework.ui.Model model = new org.springframework.ui.ExtendedModelMap();
        BoardAdminDashboardFacade.DashboardViewData data = new BoardAdminDashboardFacade.DashboardViewData(
                1L,
                2L,
                3L,
                6L,
                4L,
                java.util.List.of(new BoardCardView(10L, "popular", null, null, null, 1L, 2L, 3L, null, false, false)),
                java.util.List.of(),
                java.util.List.of()
        );
        when(boardAdminDashboardFacade.loadDashboardData()).thenReturn(data);

        String viewName = adminBoardPageController.boardDashboardPage(model);

        assertThat(viewName).isEqualTo("admin/boards/dashboard");
        assertThat(model.getAttribute("popularBoards")).isEqualTo(data.popularBoards());
    }

    @Test
    void updateReportStatus_keepsOneIndexedPageOnRedirect() {
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String viewName = adminBoardPageController.updateReportStatus(
                10L,
                BoardReportStatus.RESOLVED,
                "done",
                "resolved",
                2,
                adminAccount(99L),
                redirectAttributes
        );

        verify(boardAdminReportFacade).updateReportStatus(
                new BoardReportStatusUpdateCommand(10L, BoardReportStatus.RESOLVED, 99L, "done")
        );
        assertThat(viewName).isEqualTo("redirect:/admin/boards/reports?status=RESOLVED&page=2");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("reportMessage");
    }

    @Test
    void updateReportStatus_clampsPageToFirstPageWhenUnauthenticated() {
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String viewName = adminBoardPageController.updateReportStatus(
                10L,
                BoardReportStatus.OPEN,
                null,
                BoardReportFilter.PARAM_OPEN,
                0,
                null,
                redirectAttributes
        );

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
