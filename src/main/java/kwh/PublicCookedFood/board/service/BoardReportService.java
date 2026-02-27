package kwh.PublicCookedFood.board.service;

import org.springframework.transaction.annotation.Transactional;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardReport;
import kwh.PublicCookedFood.board.domain.BoardReportReason;
import kwh.PublicCookedFood.board.domain.BoardReportStatus;
import kwh.PublicCookedFood.board.domain.SoftDeleteState;
import kwh.PublicCookedFood.board.repository.BoardReportRepository;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import kwh.PublicCookedFood.board.repository.CommentsRepository;
import kwh.PublicCookedFood.notification.service.NotificationService;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.repository.UserRepository;
import kwh.PublicCookedFood.user.service.UserBlockService;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BoardReportService {

    private static final int MAX_DETAILS_LENGTH = 500;
    private static final int MAX_PROCESS_NOTE_LENGTH = 500;
    private static final int MAX_REPORTS_PER_10_MINUTES = 5;
    private static final int MAX_REPORTS_PER_DAY = 30;
    private static final int BURST_SUSPICIOUS_THRESHOLD = 3;
    private static final int DAILY_SUSPICIOUS_THRESHOLD = 12;
    private static final int AUTO_HIDE_OPEN_REPORT_THRESHOLD = 3;

    private final BoardReportRepository boardReportRepository;
    private final BoardRepository boardRepository;
    private final CommentsRepository commentsRepository;
    private final UserRepository userRepository;
    private final UserBlockService userBlockService;
    private final NotificationService notificationService;

    @Transactional
    public void createReport(Long boardId, Long reporterId, BoardReportReason reason, String details) {
        if (boardId == null || reporterId == null) {
            throw new IllegalArgumentException("신고 요청 값이 올바르지 않습니다.");
        }
        if (reason == null) {
            throw new IllegalArgumentException("신고 사유를 선택해주세요.");
        }
        if (reason == BoardReportReason.OTHER && (details == null || details.isBlank())) {
            throw new IllegalArgumentException("기타 사유는 상세 내용을 입력해주세요.");
        }
        if (boardReportRepository.existsByBoardIdAndReporterId(boardId, reporterId)) {
            throw new IllegalStateException("이미 신고한 게시글입니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        ReporterReportVolume reporterVolume = getReporterReportVolume(reporterId, now);
        enforceReportRateLimit(reporterVolume);

        Users reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 사용자입니다."));
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 게시글입니다."));
        if (board.getState() != SoftDeleteState.ACTIVE) {
            throw new IllegalArgumentException("삭제된 게시글은 신고할 수 없습니다.");
        }
        if (board.getUser() != null && userBlockService.isEitherBlocked(reporterId, board.getUser().getId())) {
            throw new IllegalStateException("차단 관계인 사용자의 게시글은 신고할 수 없습니다.");
        }

        if (board.getUser() != null
                && board.getUser().getId() != null
                && board.getUser().getId().equals(reporterId)) {
            throw new IllegalArgumentException("본인 게시글은 신고할 수 없습니다.");
        }

        int priorityScore = calculatePriorityScore(reporterId, reason);
        boolean suspicious = isSuspiciousReportVolume(reporterVolume);

        try {
            BoardReport savedReport = boardReportRepository.save(BoardReport.builder()
                    .board(board)
                    .reporter(reporter)
                    .reason(reason)
                    .details(normalizeDetails(details))
                    .status(BoardReportStatus.OPEN)
                    .priorityScore(applySuspiciousPenalty(priorityScore, suspicious))
                    .suspicious(suspicious)
                    .build());
            syncBoardVisibilityByReportCount(savedReport.getBoard());
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("이미 신고한 게시글입니다.");
        }
    }

    @Transactional(readOnly = true)
    public long getReportCountByStatus(BoardReportStatus status) {
        if (status == null) {
            return boardReportRepository.count();
        }
        return boardReportRepository.countByStatus(status);
    }

    @Transactional(readOnly = true)
    public boolean hasReported(Long boardId, Long reporterId) {
        if (boardId == null || reporterId == null) {
            return false;
        }
        return boardReportRepository.existsByBoardIdAndReporterId(boardId, reporterId);
    }

    @Transactional(readOnly = true)
    public Page<BoardReport> getReports(BoardReportStatus status, Pageable pageable) {
        if (status == null) {
            return boardReportRepository.findAllWithBoardAndReporter(pageable);
        }
        return boardReportRepository.findByStatusWithBoardAndReporter(status, pageable);
    }

    @Transactional
    public void updateReportStatus(Long reportId,
                                   BoardReportStatus status,
                                   Long processorId,
                                   String processNote) {
        if (reportId == null) {
            throw new IllegalArgumentException("신고 ID가 올바르지 않습니다.");
        }
        if (status == null) {
            throw new IllegalArgumentException("변경할 신고 상태가 올바르지 않습니다.");
        }

        BoardReport report = boardReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("신고 내역을 찾을 수 없습니다."));
        BoardReportStatus previousStatus = report.getStatus();

        Users processor = null;
        if (status != BoardReportStatus.OPEN) {
            if (processorId == null) {
                throw new IllegalArgumentException("처리자 정보가 없습니다.");
            }
            processor = userRepository.findById(processorId)
                    .orElseThrow(() -> new IllegalArgumentException("처리자 정보를 찾을 수 없습니다."));
        }

        report.updateStatus(status, processor, normalizeProcessNote(processNote), LocalDateTime.now());
        syncBoardVisibilityByReportCount(report.getBoard());

        if (previousStatus != status && status != BoardReportStatus.OPEN) {
            notificationService.notifyOnReportProcessed(report);
        }
    }

    private String normalizeDetails(String details) {
        if (details == null || details.isBlank()) {
            return null;
        }
        String sanitized = Jsoup.clean(details, Safelist.none()).trim().replaceAll("\\s+", " ");
        if (sanitized.isBlank()) {
            return null;
        }
        if (sanitized.length() <= MAX_DETAILS_LENGTH) {
            return sanitized;
        }
        return sanitized.substring(0, MAX_DETAILS_LENGTH);
    }

    private String normalizeProcessNote(String note) {
        if (note == null || note.isBlank()) {
            return null;
        }
        String sanitized = Jsoup.clean(note, Safelist.none()).trim().replaceAll("\\s+", " ");
        if (sanitized.isBlank()) {
            return null;
        }
        if (sanitized.length() <= MAX_PROCESS_NOTE_LENGTH) {
            return sanitized;
        }
        return sanitized.substring(0, MAX_PROCESS_NOTE_LENGTH);
    }

    private ReporterReportVolume getReporterReportVolume(Long reporterId, LocalDateTime now) {
        LocalDateTime tenMinutesAgo = now.minusMinutes(10);
        LocalDateTime oneDayAgo = now.minusDays(1);
        long recentTenMinuteCount = boardReportRepository.countByReporterIdAndRegTimeAfter(reporterId, tenMinutesAgo);
        long recentDailyCount = boardReportRepository.countByReporterIdAndRegTimeAfter(reporterId, oneDayAgo);
        return new ReporterReportVolume(recentTenMinuteCount, recentDailyCount);
    }

    private void enforceReportRateLimit(ReporterReportVolume volume) {
        if (volume.recentTenMinuteCount() >= MAX_REPORTS_PER_10_MINUTES) {
            throw new IllegalStateException("신고 요청이 너무 빠릅니다. 잠시 후 다시 시도해주세요.");
        }
        if (volume.recentDailyCount() >= MAX_REPORTS_PER_DAY) {
            throw new IllegalStateException("하루 신고 가능 횟수를 초과했습니다.");
        }
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

        long activeBoardCount = boardRepository.countByUserIdAndState(reporterId, SoftDeleteState.ACTIVE);
        long activeCommentCount = commentsRepository.countByUserIdAndState(reporterId, SoftDeleteState.ACTIVE);

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

    private void syncBoardVisibilityByReportCount(Board board) {
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

    private record ReporterReportVolume(long recentTenMinuteCount, long recentDailyCount) {
    }
}
