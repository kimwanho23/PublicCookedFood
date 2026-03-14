package kwh.PublicCookedFood.board.service.query;

import kwh.PublicCookedFood.board.domain.BoardReport;
import kwh.PublicCookedFood.board.domain.BoardReportStatus;
import kwh.PublicCookedFood.board.repository.BoardReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BoardReportQueryService {

    private final BoardReportRepository boardReportRepository;

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
        return boardReportRepository.existsByBoardIdAndReporterIdAndStatus(boardId, reporterId, BoardReportStatus.OPEN);
    }

    @Transactional(readOnly = true)
    public Page<BoardReport> getReports(BoardReportStatus status, Pageable pageable) {
        if (status == null) {
            return boardReportRepository.findAllWithBoardAndReporter(pageable);
        }
        return boardReportRepository.findByStatusWithBoardAndReporter(status, pageable);
    }
}
