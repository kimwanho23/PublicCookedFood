package kwh.PublicCookedFood.board.controller;

import kwh.PublicCookedFood.board.service.query.BoardReportFilter;

public final class BoardReportPageRedirect {

    private final BoardReportFilter filter;
    private final int page;

    public BoardReportPageRedirect(BoardReportFilter filter, int page) {
        this.filter = filter == null ? BoardReportFilter.OPEN : filter;
        this.page = Math.max(page, 1);
    }

    public static BoardReportPageRedirect of(String rawFilter, int page) {
        return new BoardReportPageRedirect(BoardReportFilter.from(rawFilter), page);
    }

    public BoardReportFilter filter() {
        return filter;
    }

    public int page() {
        return page;
    }

    public String viewName() {
        return "redirect:/admin/boards/reports?status=" + filter.paramValue() + "&page=" + page;
    }
}
