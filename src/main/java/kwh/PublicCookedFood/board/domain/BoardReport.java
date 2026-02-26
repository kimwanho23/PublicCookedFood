package kwh.PublicCookedFood.board.domain;

import jakarta.persistence.*;
import kwh.PublicCookedFood.common.BaseTimeEntity;
import kwh.PublicCookedFood.user.domain.Users;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "board_report", uniqueConstraints = {
        @UniqueConstraint(name = "uk_board_report_board_reporter", columnNames = {"board_id", "reporter_id"})
}, indexes = {
        @Index(name = "idx_board_report_status_regtime", columnList = "status, regTime"),
        @Index(name = "idx_board_report_reporter_regtime", columnList = "reporter_id, regTime"),
        @Index(name = "idx_board_report_board_regtime", columnList = "board_id, regTime")
})
public class BoardReport extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", referencedColumnName = "id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Board board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", referencedColumnName = "id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Users reporter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BoardReportReason reason;

    @Column(name = "details", length = 500)
    private String details;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BoardReportStatus status = BoardReportStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by", referencedColumnName = "id")
    private Users processor;

    @Column(name = "processed_note", length = 500)
    private String processedNote;

    @Column(name = "processed_time")
    private LocalDateTime processedTime;

    @Column(name = "priority_score", nullable = false)
    private Integer priorityScore = 0;

    @Column(name = "is_suspicious", nullable = false)
    private boolean suspicious = false;

    @Builder
    public BoardReport(Long id,
                       Board board,
                       Users reporter,
                       BoardReportReason reason,
                       String details,
                       BoardReportStatus status,
                       Users processor,
                       String processedNote,
                       LocalDateTime processedTime,
                       Integer priorityScore,
                       boolean suspicious) {
        this.id = id;
        this.board = board;
        this.reporter = reporter;
        this.reason = reason;
        this.details = details;
        this.status = status == null ? BoardReportStatus.OPEN : status;
        this.processor = processor;
        this.processedNote = processedNote;
        this.processedTime = processedTime;
        this.priorityScore = priorityScore == null ? 0 : Math.max(priorityScore, 0);
        this.suspicious = suspicious;
    }

    public void updateStatus(BoardReportStatus status) {
        updateStatus(status, null, null, null);
    }

    public void updateStatus(BoardReportStatus status, Users processor, String processedNote, LocalDateTime processedTime) {
        BoardReportStatus normalizedStatus = status == null ? BoardReportStatus.OPEN : status;
        this.status = normalizedStatus;

        if (normalizedStatus == BoardReportStatus.OPEN) {
            this.processor = null;
            this.processedNote = null;
            this.processedTime = null;
            return;
        }

        this.processor = processor;
        this.processedNote = processedNote;
        this.processedTime = processedTime == null ? LocalDateTime.now() : processedTime;
    }

    public void applyRiskSignals(Integer priorityScore, boolean suspicious) {
        this.priorityScore = priorityScore == null ? 0 : Math.max(priorityScore, 0);
        this.suspicious = suspicious;
    }
}
