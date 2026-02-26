package kwh.PublicCookedFood.board.contoller;

import io.swagger.v3.oas.annotations.Hidden;
import kwh.PublicCookedFood.board.domain.BoardPolicy;
import kwh.PublicCookedFood.board.domain.BoardReport;
import kwh.PublicCookedFood.board.domain.BoardReportStatus;
import kwh.PublicCookedFood.board.domain.BoardThumbnailDisplayMode;
import kwh.PublicCookedFood.board.dto.response.BoardPolicyResponse;
import kwh.PublicCookedFood.board.service.BoardReportService;
import kwh.PublicCookedFood.board.service.BoardPolicyService;
import kwh.PublicCookedFood.board.service.BoardService;
import kwh.PublicCookedFood.config.oauth2.LoginUser;
import kwh.PublicCookedFood.food.service.RecipeReviewService;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.service.UserActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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

import java.util.Locale;
import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/boards")
@Hidden
public class AdminBoardPageController {

    private static final int MIN_FEATURED_THRESHOLD = 1;
    private static final int MAX_FEATURED_THRESHOLD = 10000;
    private static final String REPORT_FILTER_ALL = "ALL";
    private static final String REPORT_FILTER_OPEN = "OPEN";

    private final BoardPolicyService boardPolicyService;
    private final BoardReportService boardReportService;
    private final BoardService boardService;
    private final RecipeReviewService recipeReviewService;
    private final UserActivityLogService userActivityLogService;

    @GetMapping("/dashboard")
    public String boardDashboardPage(Model model) {
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        LocalDateTime monthAgo = LocalDateTime.now().minusDays(30);

        model.addAttribute("openCount", boardReportService.getReportCountByStatus(BoardReportStatus.OPEN));
        model.addAttribute("resolvedCount", boardReportService.getReportCountByStatus(BoardReportStatus.RESOLVED));
        model.addAttribute("rejectedCount", boardReportService.getReportCountByStatus(BoardReportStatus.REJECTED));
        model.addAttribute("allCount", boardReportService.getReportCountByStatus(null));
        model.addAttribute("hiddenByReportCount", boardService.getHiddenByReportCount());
        model.addAttribute("popularBoards", boardService.getPopularBoardsSince(weekAgo, 8));
        model.addAttribute("reviewRankings", recipeReviewService.getTopReviewRankings(monthAgo, 8));
        model.addAttribute("recentActivities", userActivityLogService.getRecentActivities(15));
        return "admin/boards/dashboard";
    }

    @GetMapping("/policy")
    public String boardPolicyPage(Model model) {
        BoardPolicy policy = boardPolicyService.getPolicy();
        model.addAttribute("policy", BoardPolicyResponse.from(policy));
        model.addAttribute("minFeaturedThreshold", MIN_FEATURED_THRESHOLD);
        model.addAttribute("maxFeaturedThreshold", MAX_FEATURED_THRESHOLD);
        return "admin/boards/policy";
    }

    @PostMapping("/policy")
    public String updateBoardPolicy(
            @RequestParam(value = "featuredLikeThreshold", required = false) Integer featuredLikeThreshold,
            @RequestParam(value = "thumbnailDisplayMode", required = false) BoardThumbnailDisplayMode thumbnailDisplayMode,
            @LoginUser Users user,
            RedirectAttributes redirectAttributes
    ) {
        int normalizedThreshold = normalizeFeaturedThreshold(featuredLikeThreshold);
        BoardThumbnailDisplayMode normalizedDisplayMode = thumbnailDisplayMode == null
                ? boardPolicyService.getThumbnailDisplayMode()
                : thumbnailDisplayMode;

        boardPolicyService.updateFeaturedLikeThreshold(normalizedThreshold);
        boardPolicyService.updateThumbnailDisplayMode(normalizedDisplayMode);
        if (user != null && user.getId() != null) {
            userActivityLogService.record(
                    user.getId(),
                    "BOARD_POLICY_UPDATE",
                    "featuredLikeThreshold=" + normalizedThreshold + ",thumbnailDisplayMode=" + normalizedDisplayMode.name()
            );
        }

        redirectAttributes.addFlashAttribute("policyMessage", "게시판 정책을 저장했습니다.");
        return "redirect:/admin/boards/policy";
    }

    @GetMapping("/reports")
    public String boardReportPage(
            @RequestParam(value = "status", required = false, defaultValue = REPORT_FILTER_OPEN) String statusFilter,
            @PageableDefault(page = 0, size = 20, sort = "regTime", direction = Sort.Direction.DESC) Pageable pageable,
            Model model
    ) {
        String normalizedFilter = normalizeReportFilter(statusFilter);
        BoardReportStatus selectedStatus = parseReportStatus(normalizedFilter);
        Page<BoardReport> reportPage = boardReportService.getReports(selectedStatus, pageable);

        model.addAttribute("reportPage", reportPage);
        model.addAttribute("reportFilter", normalizedFilter);
        model.addAttribute("openCount", boardReportService.getReportCountByStatus(BoardReportStatus.OPEN));
        model.addAttribute("resolvedCount", boardReportService.getReportCountByStatus(BoardReportStatus.RESOLVED));
        model.addAttribute("rejectedCount", boardReportService.getReportCountByStatus(BoardReportStatus.REJECTED));
        model.addAttribute("allCount", boardReportService.getReportCountByStatus(null));
        model.addAttribute("reportStatusValues", BoardReportStatus.values());
        return "admin/boards/reports";
    }

    @PostMapping("/reports/{reportId}/status")
    public String updateReportStatus(
            @PathVariable Long reportId,
            @RequestParam("status") BoardReportStatus status,
            @RequestParam(value = "processNote", required = false) String processNote,
            @RequestParam(value = "redirectStatus", required = false, defaultValue = REPORT_FILTER_OPEN) String redirectStatus,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @LoginUser Users user,
            RedirectAttributes redirectAttributes
    ) {
        if (user == null || user.getId() == null) {
            redirectAttributes.addFlashAttribute("reportErrorMessage", "신고 처리 권한이 없습니다.");
            return "redirect:/admin/boards/reports?status=" + normalizeReportFilter(redirectStatus) + "&page=" + Math.max(page, 0);
        }
        try {
            boardReportService.updateReportStatus(reportId, status, user.getId(), processNote);
            userActivityLogService.record(
                    user.getId(),
                    "BOARD_REPORT_STATUS_UPDATE",
                    "reportId=" + reportId + ",status=" + status.name()
            );
            redirectAttributes.addFlashAttribute("reportMessage", "신고 상태를 변경했습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("reportErrorMessage", e.getMessage());
        }
        return "redirect:/admin/boards/reports?status=" + normalizeReportFilter(redirectStatus) + "&page=" + Math.max(page, 0);
    }

    private int normalizeFeaturedThreshold(Integer featuredLikeThreshold) {
        if (featuredLikeThreshold == null) {
            return boardPolicyService.getFeaturedLikeThreshold();
        }
        if (featuredLikeThreshold < MIN_FEATURED_THRESHOLD) {
            return MIN_FEATURED_THRESHOLD;
        }
        if (featuredLikeThreshold > MAX_FEATURED_THRESHOLD) {
            return MAX_FEATURED_THRESHOLD;
        }
        return featuredLikeThreshold;
    }

    private String normalizeReportFilter(String statusFilter) {
        if (statusFilter == null || statusFilter.isBlank()) {
            return REPORT_FILTER_OPEN;
        }
        String normalized = statusFilter.trim().toUpperCase(Locale.ROOT);
        if (REPORT_FILTER_ALL.equals(normalized)) {
            return REPORT_FILTER_ALL;
        }
        try {
            BoardReportStatus.valueOf(normalized);
            return normalized;
        } catch (IllegalArgumentException ignored) {
            return REPORT_FILTER_OPEN;
        }
    }

    private BoardReportStatus parseReportStatus(String normalizedFilter) {
        if (REPORT_FILTER_ALL.equals(normalizedFilter)) {
            return null;
        }
        return BoardReportStatus.valueOf(normalizedFilter);
    }
}
