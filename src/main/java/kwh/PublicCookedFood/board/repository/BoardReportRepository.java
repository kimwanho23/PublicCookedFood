package kwh.PublicCookedFood.board.repository;

import kwh.PublicCookedFood.board.domain.BoardReport;
import kwh.PublicCookedFood.board.domain.BoardReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface BoardReportRepository extends JpaRepository<BoardReport, Long> {

    boolean existsByBoardIdAndReporterId(Long boardId, Long reporterId);

    long countByBoardId(Long boardId);

    long countByBoardIdAndStatus(Long boardId, BoardReportStatus status);

    long countByStatus(BoardReportStatus status);

    long countByReporterIdAndRegTimeAfter(Long reporterId, LocalDateTime since);

    @Query(value = "SELECT br FROM BoardReport br " +
            "JOIN FETCH br.board " +
            "JOIN FETCH br.reporter " +
            "LEFT JOIN FETCH br.processor " +
            "ORDER BY br.suspicious DESC, br.priorityScore DESC, br.regTime DESC",
            countQuery = "SELECT COUNT(br) FROM BoardReport br")
    Page<BoardReport> findAllWithBoardAndReporter(Pageable pageable);

    @Query(value = "SELECT br FROM BoardReport br " +
            "JOIN FETCH br.board " +
            "JOIN FETCH br.reporter " +
            "LEFT JOIN FETCH br.processor " +
            "WHERE br.status = :status " +
            "ORDER BY br.suspicious DESC, br.priorityScore DESC, br.regTime DESC",
            countQuery = "SELECT COUNT(br) FROM BoardReport br WHERE br.status = :status")
    Page<BoardReport> findByStatusWithBoardAndReporter(@Param("status") BoardReportStatus status, Pageable pageable);
}
