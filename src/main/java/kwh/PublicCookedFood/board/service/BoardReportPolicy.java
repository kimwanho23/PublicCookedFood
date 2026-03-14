package kwh.PublicCookedFood.board.service;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardReportReason;
import kwh.PublicCookedFood.board.domain.BoardReportStatus;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import kwh.PublicCookedFood.board.error.BoardErrorCode;
import kwh.PublicCookedFood.board.repository.BoardReportRepository;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import kwh.PublicCookedFood.board.repository.CommentsRepository;
import kwh.PublicCookedFood.board.service.command.BoardReportCreateCommand;
import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.common.error.CommonErrorCode;
import kwh.PublicCookedFood.account.service.AccountBlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Objects;

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

    public void validateCreateRequest(BoardReportCreateCommand command) {
        if (command.reason() == BoardReportReason.OTHER
                && (command.details() == null || command.details().trim().isEmpty())) {
            throw new AppException(CommonErrorCode.INVALID_REQUEST, "기타 사유는 상세 내용을 입력해주세요.");
        }
    }

    public void ensureNotReported(BoardReportCreateCommand command) {
        if (boardReportRepository.existsByBoardIdAndReporterIdAndStatus(
                command.boardId(),
                command.reporterId(),
                BoardReportStatus.OPEN)) {
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
            throw new AppException(CommonErrorCode.INVALID_REQUEST, "삭제된 게시글은 신고할 수 없습니다.");
        }
        if (board.getAccount() != null && accountBlockService.isEitherBlocked(reporterId, board.getAccount().getId())) {
            throw new AppException(BoardErrorCode.BOARD_REPORT_BLOCKED);
        }
        if (board.getAccount() != null
                && board.getAccount().getId() != null
                && board.getAccount().getId().equals(reporterId)) {
            throw new AppException(CommonErrorCode.ACCESS_DENIED, "본인 게시글은 신고할 수 없습니다.");
        }
    }

    public ReportPriority assessPriority(BoardReportCreateCommand command,
                                         ReporterReportVolume reporterVolume) {
        boolean suspicious = isSuspiciousReportVolume(reporterVolume);
        return new ReportPriority(
                applySuspiciousPenalty(calculatePriorityScore(command.reporterId(), command.reason()), suspicious),
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
        int reasonScore;
        switch (reason) {
            case PERSONAL_INFO:
                reasonScore = 95;
                break;
            case ABUSE:
                reasonScore = 80;
                break;
            case OBSCENE:
                reasonScore = 75;
                break;
            case SPAM:
                reasonScore = 55;
                break;
            case OTHER:
                reasonScore = 50;
                break;
            default:
                throw new IllegalArgumentException("지원하지 않는 신고 사유입니다.");
        }

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

    public static final class ReporterReportVolume {
        private final long recentTenMinuteCount;
        private final long recentDailyCount;

        public ReporterReportVolume(long recentTenMinuteCount, long recentDailyCount) {
            this.recentTenMinuteCount = recentTenMinuteCount;
            this.recentDailyCount = recentDailyCount;
        }

        public long recentTenMinuteCount() {
            return recentTenMinuteCount;
        }

        public long getRecentTenMinuteCount() {
            return recentTenMinuteCount;
        }

        public long recentDailyCount() {
            return recentDailyCount;
        }

        public long getRecentDailyCount() {
            return recentDailyCount;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof ReporterReportVolume)) {
                return false;
            }
            ReporterReportVolume that = (ReporterReportVolume) other;
            return recentTenMinuteCount == that.recentTenMinuteCount
                    && recentDailyCount == that.recentDailyCount;
        }

        @Override
        public int hashCode() {
            return Objects.hash(recentTenMinuteCount, recentDailyCount);
        }
    }

    public static final class ReportPriority {
        private final int score;
        private final boolean suspicious;

        public ReportPriority(int score, boolean suspicious) {
            this.score = score;
            this.suspicious = suspicious;
        }

        public int score() {
            return score;
        }

        public int getScore() {
            return score;
        }

        public boolean suspicious() {
            return suspicious;
        }

        public boolean isSuspicious() {
            return suspicious;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof ReportPriority)) {
                return false;
            }
            ReportPriority that = (ReportPriority) other;
            return score == that.score && suspicious == that.suspicious;
        }

        @Override
        public int hashCode() {
            return Objects.hash(score, suspicious);
        }
    }
}
