package kwh.PublicCookedFood.board.application.query.view;

import kwh.PublicCookedFood.board.dto.response.BoardSectionView;
import kwh.PublicCookedFood.board.service.query.BoardListQueryNotice;
import lombok.Getter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Getter
public final class BoardListPageView {

    private final Page<BoardCardView> boardList;
    private final Pageable listPageable;
    private final String search;
    private final String activeSection;
    private final String orderBy;
    private final String thumbnailDisplayMode;
    private final List<BoardSectionView> sections;
    private final String boardListPath;
    private final boolean featuredPage;
    private final int featuredLikeThreshold;
    private final String boardPageTitle;
    private final BoardListQueryNotice queryNotice;

    public BoardListPageView(Page<BoardCardView> boardList,
                             Pageable listPageable,
                             String search,
                             String activeSection,
                             String orderBy,
                             String thumbnailDisplayMode,
                             List<BoardSectionView> sections,
                             String boardListPath,
                             boolean featuredPage,
                             int featuredLikeThreshold,
                             String boardPageTitle,
                             BoardListQueryNotice queryNotice) {
        this.boardList = Objects.requireNonNull(boardList, "boardList");
        this.listPageable = listPageable;
        this.search = search;
        this.activeSection = activeSection;
        this.orderBy = orderBy;
        this.thumbnailDisplayMode = thumbnailDisplayMode;
        this.sections = unmodifiableSections(sections);
        this.boardListPath = boardListPath;
        this.featuredPage = featuredPage;
        this.featuredLikeThreshold = featuredLikeThreshold;
        this.boardPageTitle = boardPageTitle;
        this.queryNotice = queryNotice == null ? BoardListQueryNotice.none() : queryNotice;
    }

    public boolean hasQueryNotice() {
        return queryNotice.present();
    }

    public java.util.Optional<String> maybeQueryErrorMsg() {
        return queryNotice.text();
    }

    public Page<BoardCardView> boardList() {
        return boardList;
    }

    public Pageable listPageable() {
        return listPageable;
    }

    public String search() {
        return search;
    }

    public String activeSection() {
        return activeSection;
    }

    public String orderBy() {
        return orderBy;
    }

    public String thumbnailDisplayMode() {
        return thumbnailDisplayMode;
    }

    public List<BoardSectionView> sections() {
        return sections;
    }

    public String boardListPath() {
        return boardListPath;
    }

    public boolean featuredPage() {
        return featuredPage;
    }

    public int featuredLikeThreshold() {
        return featuredLikeThreshold;
    }

    public String boardPageTitle() {
        return boardPageTitle;
    }

    public BoardListQueryNotice queryNotice() {
        return queryNotice;
    }

    private static List<BoardSectionView> unmodifiableSections(List<BoardSectionView> sections) {
        if (sections == null || sections.isEmpty()) {
            return Collections.emptyList();
        }
        return List.copyOf(sections);
    }
}
