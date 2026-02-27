package kwh.PublicCookedFood.board.contoller;

import io.swagger.v3.oas.annotations.Hidden;
import kwh.PublicCookedFood.board.domain.BoardReportStatus;
import kwh.PublicCookedFood.board.domain.BoardThumbnailDisplayMode;
import kwh.PublicCookedFood.board.facade.BoardAdminFacade;
import kwh.PublicCookedFood.config.oauth2.LoginUser;
import kwh.PublicCookedFood.user.domain.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/boards")
@Hidden
public class AdminBoardPageController {

    private final BoardAdminFacade boardAdminFacade;

    @GetMapping("/dashboard")
    public String boardDashboardPage(Model model) {
        BoardAdminFacade.DashboardViewData data = boardAdminFacade.loadDashboardData();
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
        BoardAdminFacade.PolicyViewData data = boardAdminFacade.loadPolicyData();
        model.addAttribute("policy", data.policy());
        model.addAttribute("minFeaturedThreshold", data.minFeaturedThreshold());
        model.addAttribute("maxFeaturedThreshold", data.maxFeaturedThreshold());
        return "admin/boards/policy";
    }

    @PostMapping("/policy")
    public String updateBoardPolicy(
            @RequestParam(value = "featuredLikeThreshold", required = false) Integer featuredLikeThreshold,
            @RequestParam(value = "thumbnailDisplayMode", required = false) BoardThumbnailDisplayMode thumbnailDisplayMode,
            @LoginUser Users user,
            RedirectAttributes redirectAttributes
    ) {
        boardAdminFacade.updateBoardPolicy(
                featuredLikeThreshold,
                thumbnailDisplayMode,
                user == null ? null : user.getId()
        );
        redirectAttributes.addFlashAttribute("policyMessage", "게시판 정책을 저장했습니다.");
        return "redirect:/admin/boards/policy";
    }

    @GetMapping("/reports")
    public String boardReportPage(
            @RequestParam(value = "status", required = false, defaultValue = BoardAdminFacade.REPORT_FILTER_OPEN) String statusFilter,
            @PageableDefault(page = 0, size = 20, sort = "regTime", direction = Sort.Direction.DESC) Pageable pageable,
            Model model
    ) {
        BoardAdminFacade.ReportPageViewData data = boardAdminFacade.loadReportPage(statusFilter, pageable);
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
            @RequestParam(value = "redirectStatus", required = false, defaultValue = BoardAdminFacade.REPORT_FILTER_OPEN) String redirectStatus,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @LoginUser Users user,
            RedirectAttributes redirectAttributes
    ) {
        if (user == null || user.getId() == null) {
            redirectAttributes.addFlashAttribute("reportErrorMessage", "신고 처리 권한이 없습니다.");
            return "redirect:/admin/boards/reports?status=" + boardAdminFacade.normalizeReportFilter(redirectStatus) + "&page=" + Math.max(page, 0);
        }
        try {
            boardAdminFacade.updateReportStatus(reportId, status, processNote, user.getId());
            redirectAttributes.addFlashAttribute("reportMessage", "신고 상태를 변경했습니다.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("reportErrorMessage", e.getMessage());
        }
        return "redirect:/admin/boards/reports?status=" + boardAdminFacade.normalizeReportFilter(redirectStatus) + "&page=" + Math.max(page, 0);
    }
}
