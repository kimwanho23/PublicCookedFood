package kwh.PublicCookedFood.board.repository;

import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import org.springframework.data.domain.Pageable;

import java.util.Objects;

public final class BoardSnapshotQuery {

    private final SoftDeleteState state;
    private final BoardPageQuery.FeaturedThreshold featuredThreshold;
    private final BoardPageQuery.SectionFilter section;
    private final Pageable pageable;

    public BoardSnapshotQuery(SoftDeleteState state,
                              BoardPageQuery.FeaturedThreshold featuredThreshold,
                              BoardPageQuery.SectionFilter section,
                              Pageable pageable) {
        this.state = Objects.requireNonNull(state, "state");
        this.featuredThreshold = Objects.requireNonNull(featuredThreshold, "featuredThreshold");
        this.section = section == null ? BoardPageQuery.SectionFilter.all() : section;
        this.pageable = Objects.requireNonNull(pageable, "pageable");
    }

    public SoftDeleteState state() {
        return state;
    }

    public BoardPageQuery.FeaturedThreshold featuredThreshold() {
        return featuredThreshold;
    }

    public BoardPageQuery.SectionFilter section() {
        return section;
    }

    public Pageable pageable() {
        return pageable;
    }
}
