package kwh.PublicCookedFood.board.service;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardReportReason;
import kwh.PublicCookedFood.board.domain.BoardReportStatus;
import kwh.PublicCookedFood.board.domain.SoftDeleteState;
import kwh.PublicCookedFood.board.error.BoardErrorCode;
import kwh.PublicCookedFood.board.repository.BoardReportRepository;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import kwh.PublicCookedFood.board.repository.CommentsRepository;
import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.account.service.AccountBlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class BoardReportPolicy {

    private static final int MAX_REPORTS_PER_10_MINUTES = 5;
    private static final int MAX_REPORTS_PER_DAY = 30;
    private static final int BURST_SUSPICIOUS_THRESHOLD = 3;
    private static final int DAILY_SUSPICIOUS_THRESHOLD = 12;
    private static final int AUTO_HIDE_OPEN_REPORT_THRESHOLD = 3;

    private final BoardReportRepository boardReportRepository;
    private final BoardRepository boardRepository;
    private final CommentsRepository commentsRepository;
    private final AccountBlockService accountBlockService;

    public void validateCreateRequest(Long boardId, Long reporterId, BoardReportReason reason, String details) {
        if (boardId == null || reporterId == null) {
            throw new IllegalArgumentException("신고 요청 값이 올바르지 않습니다.");
        }
        if (reason == null) {
            throw new IllegalArgumentException("신고 사유를 선택해주세요.");
        }
        if (reason == BoardReportReason.OTHER && (details == null || details.isBlank())) {
            throw new IllegalArgumentException("기타 사유는 상세 내용을 입력해주세요.");
        }
    }

    public void ensureNotReported(Long boardId, Long reporterId) {
        if (boardReportRepository.existsByBoardIdAndReporterIdAndStatus(boardId, reporterId, BoardReportStatus.OPEN)) {
            throw new AppException(BoardErrorCode.BOARD_REPORT_DUPLICATED);
        }
    }

    public ReporterReportVolume loadReporterVolume(Long reporterId, LocalDateTime now) {
        LocalDateTime tenMinutesAgo = now.minusMinutes(10);
        LocalDateTime oneDayAgo = now.minusDays(1);
        long recentTenMinuteCount = boardReportRepository.countByReporterIdAndRegTimeAfter(reporterId, tenMinutesAgo);
        long recentDailyCount = boardReportRepository.countByReporterIdAndRegTimeAfter(reporterId, oneDayAgo);
        return new ReporterReportVolume(recentTenMinuteCount, recentDailyCount);
    }

    public void enforceRateLimit(ReporterReportVolume volume) {
        if (volume.recentTenMinuteCount() >= MAX_REPORTS_PER_10_MINUTES) {
            throw new AppException(BoardErrorCode.BOARD_REPORT_RATE_LIMITED);
        }
        if (volume.recentDailyCount() >= MAX_REPORTS_PER_DAY) {
            throw new AppException(BoardErrorCode.BOARD_REPORT_DAILY_LIMIT_EXCEEDED);
        }
    }

    public void ensureReportable(Board board, Long reporterId) {
        if (board.getState() != SoftDeleteState.ACTIVE) {
            throw new IllegalArgumentException("삭제된 게시글은 신고할 수 없습니다.");
        }
        if (board.getAccount() != null && accountBlockService.isEitherBlocked(reporterId, board.getAccount().getId())) {
            throw new AppException(BoardErrorCode.BOARD_REPORT_BLOCKED);
        }
        if (board.getAccount() != null
                && board.getAccount().getId() != null
                && board.getAccount().getId().equals(reporterId)) {
            throw new IllegalArgumentException("본인 게시글은 신고할 수 없습니다.");
        }
    }

    public ReportPriority assessPriority(Long reporterId,
                                         BoardReportReason reason,
                                         ReporterReportVolume reporterVolume) {
        boolean suspicious = isSuspiciousReportVolume(reporterVolume);
        return new ReportPriority(
                applySuspiciousPenalty(calculatePriorityScore(reporterId, reason), suspicious),
                suspicious
        );
    }

    public void syncBoardVisibility(Board board) {
        if (board == null || board.getId() == null || board.getState() != SoftDeleteState.ACTIVE) {
            return;
        }
        long openReports = boardReportRepository.countByBoardIdAndStatus(board.getId(), BoardReportStatus.OPEN);
        boolean shouldHide = openReports >= AUTO_HIDE_OPEN_REPORT_THRESHOLD;
        if (board.isHiddenByReport() == shouldHide) {
            return;
        }
        board.updateHiddenByReport(shouldHide);
    }

    private boolean isSuspiciousReportVolume(ReporterReportVolume volume) {
        return volume.recentTenMinuteCount() >= BURST_SUSPICIOUS_THRESHOLD
                || volume.recentDailyCount() >= DAILY_SUSPICIOUS_THRESHOLD;
    }

    private int calculatePriorityScore(Long reporterId, BoardReportReason reason) {
        int reasonScore = switch (reason) {
            case PERSONAL_INFO -> 95;
            case ABUSE -> 80;
            case OBSCENE -> 75;
            case SPAM -> 55;
            case OTHER -> 50;
        };

        long activeBoardCount = boardRepository.countByAccountIdAndState(reporterId, SoftDeleteState.ACTIVE);
        long activeCommentCount = commentsRepository.countByAccountIdAndState(reporterId, SoftDeleteState.ACTIVE);
        int boardContribution = (int) Math.min(activeBoardCount, 5L) * 2;
        int commentContribution = (int) (Math.min(activeCommentCount, 20L) / 2);
        int trustScore = Math.min(20, boardContribution + commentContribution);

        return reasonScore + trustScore;
    }

    private int applySuspiciousPenalty(int priorityScore, boolean suspicious) {
        if (!suspicious) {
            return Math.max(priorityScore, 0);
        }
        return Math.max(priorityScore - 15, 0);
    }

    public record ReporterReportVolume(long recentTenMinuteCount, long recentDailyCount) {
    }

    public record ReportPriority(int score, boolean suspicious) {
    }
}
