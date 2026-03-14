package kwh.PublicCookedFood.board.service.command;

import kwh.PublicCookedFood.board.domain.BoardReportStatus;

import java.util.Objects;

public final class BoardReportStatusUpdateCommand {

    private final long reportId;
    private final BoardReportStatus status;
    private final long processorId;
    private final String processNote;

    public BoardReportStatusUpdateCommand(long reportId,
                                          BoardReportStatus status,
                                          long processorId,
                                          String processNote) {
        if (reportId <= 0) {
            throw new IllegalArgumentException("신고 ID가 올바르지 않습니다.");
        }
        if (status == null) {
            throw new IllegalArgumentException("변경할 신고 상태가 올바르지 않습니다.");
        }
        if (processorId <= 0) {
            throw new IllegalArgumentException("처리자 정보가 없습니다.");
        }
        this.reportId = reportId;
        this.status = status;
        this.processorId = processorId;
        this.processNote = processNote;
    }

    public long reportId() {
        return reportId;
    }

    public BoardReportStatus status() {
        return status;
    }

    public long processorId() {
        return processorId;
    }

    public String processNote() {
        return processNote;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BoardReportStatusUpdateCommand)) {
            return false;
        }
        BoardReportStatusUpdateCommand that = (BoardReportStatusUpdateCommand) other;
        return reportId == that.reportId
                && processorId == that.processorId
                && status == that.status
                && Objects.equals(processNote, that.processNote);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reportId, status, processorId, processNote);
    }
}
