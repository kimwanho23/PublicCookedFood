package kwh.PublicCookedFood.board.controller;

import io.swagger.v3.oas.annotations.Hidden;
import kwh.PublicCookedFood.board.domain.BoardReportStatus;
import kwh.PublicCookedFood.board.domain.BoardThumbnailDisplayMode;
import kwh.PublicCookedFood.board.facade.BoardAdminDashboardFacade;
import kwh.PublicCookedFood.board.facade.BoardAdminPolicyFacade;
import kwh.PublicCookedFood.board.facade.BoardAdminReportFacade;
import kwh.PublicCookedFood.board.service.command.BoardPolicyUpdateCommand;
import kwh.PublicCookedFood.board.service.command.BoardReportStatusUpdateCommand;
import kwh.PublicCookedFood.board.service.query.BoardReportFilter;
import kwh.PublicCookedFood.board.service.query.BoardReportPageQuery;
import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.config.oauth2.LoginAccount;
import kwh.PublicCookedFood.account.domain.Account;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/boards")
@Hidden
public class AdminBoardPageController {

    private static final int FIRST_PAGE_NUMBER = 1;

    private final BoardAdminDashboardFacade boardAdminDashboardFacade;
    private final BoardAdminPolicyFacade boardAdminPolicyFacade;
    private final BoardAdminReportFacade boardAdminReportFacade;

    @GetMapping("")
    public String boardAdminRoot() {
        return "redirect:/admin/boards/dashboard";
    }

    @GetMapping("/dashboard")
    public String boardDashboardPage(Model model) {
        BoardAdminDashboardFacade.DashboardViewData data = boardAdminDashboardFacade.loadDashboardData();
        model.addAttribute("openCount", data.openCount());
        model.addAttribute("resolvedCount", data.resolvedCount());
        model.addAttribute("rejectedCount", data.rejectedCount());
        model.addAttribute("allCount", data.allCount());
        model.addAttribute("hiddenByReportCount", data.hiddenByReportCount());
        model.addAttribute("popularBoards", data.popularBoards());
        model.addAttribute("reviewRankings", data.reviewRankings());
        model.addAttribute("recentActivities", data.recentActivities());
        return "admin/boards/dashboard";
    }

    @GetMapping("/policy")
    public String boardPolicyPage(Model model) {
        BoardAdminPolicyFacade.PolicyViewData data = boardAdminPolicyFacade.loadPolicyData();
        model.addAttribute("policy", data.policy());
        model.addAttribute("minFeaturedThreshold", data.minFeaturedThreshold());
        model.addAttribute("maxFeaturedThreshold", data.maxFeaturedThreshold());
        return "admin/boards/policy";
    }

    @PostMapping("/policy")
    public String updateBoardPolicy(
            @RequestParam(value = "featuredLikeThreshold", required = false) Integer featuredLikeThreshold,
            @RequestParam(value = "thumbnailDisplayMode", required = false) BoardThumbnailDisplayMode thumbnailDisplayMode,
            @LoginAccount Account account,
            RedirectAttributes redirectAttributes
    ) {
        Long actorAccountId = resolveActorAccountId(account);
        boardAdminPolicyFacade.updateBoardPolicy(
                BoardPolicyUpdateCommand.fromForm(
                        featuredLikeThreshold,
                        thumbnailDisplayMode,
                        actorAccountId
                )
        );
        redirectAttributes.addFlashAttribute("policyMessage", "게시판 정책을 저장했습니다.");
        return "redirect:/admin/boards/policy";
    }

    @GetMapping("/reports")
    public String boardReportPage(
            @RequestParam(value = "status", required = false, defaultValue = BoardReportFilter.PARAM_OPEN) String statusFilter,
            @PageableDefault(page = 0, size = 20, sort = "regTime", direction = Sort.Direction.DESC) Pageable pageable,
            Model model
    ) {
        BoardAdminReportFacade.ReportPageViewData data =
                boardAdminReportFacade.loadReportPage(BoardReportPageQuery.of(statusFilter, pageable));
        model.addAttribute("reportPage", data.reportPage());
        model.addAttribute("reportFilter", data.reportFilter());
        model.addAttribute("openCount", data.openCount());
        model.addAttribute("resolvedCount", data.resolvedCount());
        model.addAttribute("rejectedCount", data.rejectedCount());
        model.addAttribute("allCount", data.allCount());
        model.addAttribute("reportStatusValues", data.reportStatusValues());
        return "admin/boards/reports";
    }

    @PostMapping("/reports/{reportId}/status")
    public String updateReportStatus(
            @PathVariable Long reportId,
            @RequestParam("status") BoardReportStatus status,
            @RequestParam(value = "processNote", required = false) String processNote,
            @RequestParam(value = "redirectStatus", required = false, defaultValue = BoardReportFilter.PARAM_OPEN) String redirectStatus,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @LoginAccount Account account,
            RedirectAttributes redirectAttributes
    ) {
        BoardReportPageRedirect redirect = BoardReportPageRedirect.of(redirectStatus, page);
        Long actorAccountId = resolveActorAccountId(account);

        if (actorAccountId == null) {
            redirectAttributes.addFlashAttribute("reportErrorMessage", "신고 처리 권한이 없습니다.");
            return redirect.viewName();
        }

        try {
            boardAdminReportFacade.updateReportStatus(
                    new BoardReportStatusUpdateCommand(reportId, status, actorAccountId, processNote)
            );
            redirectAttributes.addFlashAttribute("reportMessage", "신고 상태를 변경했습니다.");
        } catch (AppException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("reportErrorMessage", e.getMessage());
        }
        return redirect.viewName();
    }

    private Long resolveActorAccountId(Account account) {
        if (account == null || account.getId() == null) {
            return null;
        }
        return account.getId();
    }
}
