package kwh.PublicCookedFood.board.service.comment;

import kwh.PublicCookedFood.board.service.CommentNavigationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Objects;

public final class CommentPageSpec {

    private final int pageNumber;
    private final int pageSize;

    public CommentPageSpec(int pageNumber, int pageSize) {
        this.pageNumber = Math.max(pageNumber, 0);
        this.pageSize = Math.min(
                Math.max(pageSize, 1),
                CommentNavigationService.MAX_COMMENT_PAGE_SIZE
        );
    }

    public static CommentPageSpec defaultSize() {
        return new CommentPageSpec(0, CommentNavigationService.DEFAULT_COMMENT_PAGE_SIZE);
    }

    public static CommentPageSpec forTargetPath(int pageSize) {
        return new CommentPageSpec(0, pageSize);
    }

    public Pageable toPageable() {
        return PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Order.asc("regTime")));
    }

    public int pageNumber() {
        return pageNumber;
    }

    public int pageSize() {
        return pageSize;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CommentPageSpec)) {
            return false;
        }
        CommentPageSpec that = (CommentPageSpec) other;
        return pageNumber == that.pageNumber && pageSize == that.pageSize;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pageNumber, pageSize);
    }
}
