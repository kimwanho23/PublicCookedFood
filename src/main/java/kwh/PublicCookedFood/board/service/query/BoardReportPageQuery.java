package kwh.PublicCookedFood.board.service.query;

import org.springframework.data.domain.Pageable;

public final class BoardReportPageQuery {

    private final BoardReportFilter filter;
    private final Pageable pageable;

    public BoardReportPageQuery(BoardReportFilter filter, Pageable pageable) {
        this.filter = filter == null ? BoardReportFilter.OPEN : filter;
        this.pageable = pageable;
    }

    public static BoardReportPageQuery of(String statusFilter, Pageable pageable) {
        return new BoardReportPageQuery(BoardReportFilter.from(statusFilter), pageable);
    }

    public BoardReportFilter filter() {
        return filter;
    }

    public Pageable pageable() {
        return pageable;
    }
}
