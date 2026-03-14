package kwh.PublicCookedFood.board.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kwh.PublicCookedFood.account.domain.QAccount;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.QBoard;
import kwh.PublicCookedFood.board.domain.QBoardSection;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import kwh.PublicCookedFood.board.domain.QBoardStats;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class BoardRepositoryImpl implements BoardRepositoryCustom {

    private static final QBoard BOARD = QBoard.board;
    private static final QAccount ACCOUNT = QAccount.account;
    private static final QBoardSection SECTION = QBoardSection.boardSection;
    private static final QBoardStats BOARD_STATS = QBoardStats.boardStats;
    private static final PathBuilder<Board> BOARD_PATH = new PathBuilder<>(Board.class, BOARD.getMetadata());

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Board> findPageWithAccount(BoardPageQuery query) {
        BoardPageQuery safeQuery = java.util.Objects.requireNonNull(query, "query");
        BooleanBuilder where = basePredicates(safeQuery);

        JPAQuery<Board> contentQuery = queryFactory.selectFrom(BOARD)
                .join(BOARD.account, ACCOUNT).fetchJoin()
                .leftJoin(BOARD.section, SECTION).fetchJoin();

        if (safeQuery.order().usesStatsJoin()) {
            contentQuery.leftJoin(BOARD_STATS).on(BOARD_STATS.boardId.eq(BOARD.id));
        }

        List<Board> content = applyOrdering(contentQuery.where(where), safeQuery.pageable(), safeQuery.order())
                .offset(safeQuery.pageable().getOffset())
                .limit(safeQuery.pageable().getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory.select(BOARD.count())
                .from(BOARD)
                .join(BOARD.account, ACCOUNT)
                .leftJoin(BOARD.section, SECTION);

        if (safeQuery.order().requiresStatsPredicate()) {
            countQuery.leftJoin(BOARD_STATS).on(BOARD_STATS.boardId.eq(BOARD.id));
        }

        return PageableExecutionUtils.getPage(
                content,
                safeQuery.pageable(),
                () -> Optional.ofNullable(countQuery.where(where).fetchOne()).orElse(0L)
        );
    }

    @Override
    public List<Long> findTopBoardIdsForSnapshot(BoardSnapshotQuery query) {
        BoardSnapshotQuery safeQuery = java.util.Objects.requireNonNull(query, "query");
        BooleanBuilder where = basePredicates(new BoardPageQuery(
                safeQuery.state(),
                BoardPageQuery.Order.FEATURED,
                BoardPageQuery.SearchFilter.none(),
                safeQuery.section(),
                BoardPageQuery.AuthorFilter.any(),
                BoardPageQuery.VisibilityFilter.visibleToAll(),
                safeQuery.featuredThreshold(),
                safeQuery.pageable()
        ));

        return queryFactory
                .select(BOARD.id)
                .from(BOARD)
                .leftJoin(BOARD.section, SECTION)
                .leftJoin(BOARD_STATS).on(BOARD_STATS.boardId.eq(BOARD.id))
                .where(where)
                .orderBy(totalLikes().desc(), totalComments().desc(), totalViews().desc(), BOARD.regTime.desc())
                .offset(safeQuery.pageable().getOffset())
                .limit(safeQuery.pageable().getPageSize())
                .fetch();
    }

    @Override
    public List<Board> findTopByStateAndRegTimeAfterOrderByPopularity(SoftDeleteState state,
                                                                      LocalDateTime since,
                                                                      Pageable pageable) {
        BooleanBuilder where = new BooleanBuilder();
        where.and(BOARD.state.eq(state));
        where.and(BOARD.hiddenByReport.isFalse());
        where.and(BOARD.regTime.goe(since));

        return queryFactory
                .selectFrom(BOARD)
                .join(BOARD.account, ACCOUNT).fetchJoin()
                .leftJoin(BOARD.section, SECTION).fetchJoin()
                .leftJoin(BOARD_STATS).on(BOARD_STATS.boardId.eq(BOARD.id))
                .where(where)
                .orderBy(totalLikes().desc(), totalComments().desc(), totalViews().desc(), BOARD.regTime.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    private BooleanBuilder basePredicates(BoardPageQuery query) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(BOARD.state.eq(query.state()));
        builder.and(BOARD.hiddenByReport.isFalse());

        if (query.visibility().excludeBlocked() && !query.visibility().blockedAccountIds().isEmpty()) {
            builder.and(ACCOUNT.id.notIn(query.visibility().blockedAccountIds()));
        }
        query.author().authorId().ifPresent(authorId -> builder.and(ACCOUNT.id.eq(authorId)));
        query.section().sectionKey().ifPresent(sectionKey -> builder.and(SECTION.sectionKey.eq(sectionKey)));
        query.search().searchText().ifPresent(search -> builder.and(BOARD.title.contains(search)));
        if (query.featuredThreshold().minimumLikes().isPresent()) {
            long featuredThreshold = query.featuredThreshold().minimumLikes()
                    .orElseThrow(() -> new IllegalStateException("featured threshold must be present"));
            builder.and(totalLikes().goe(featuredThreshold));
        }
        return builder;
    }

    private JPAQuery<Board> applyOrdering(JPAQuery<Board> query, Pageable pageable, BoardPageQuery.Order queryOrder) {
        switch (queryOrder) {
            case RECENT:
                return query.orderBy(pageableOrders(pageable));
            case STATS_VIEWS:
                return query.orderBy(totalViews().desc(), BOARD.regTime.desc());
            case STATS_LIKES:
                return query.orderBy(totalLikes().desc(), BOARD.regTime.desc());
            case STATS_COMMENTS:
                return query.orderBy(totalComments().desc(), BOARD.regTime.desc());
            case FEATURED:
                return query.orderBy(totalLikes().desc(), totalComments().desc(), totalViews().desc(), BOARD.regTime.desc());
            default:
                throw new IllegalStateException("Unsupported board query order: " + queryOrder);
        }
    }

    private OrderSpecifier<?>[] pageableOrders(Pageable pageable) {
        if (pageable == null || pageable.getSort().isUnsorted()) {
            return new OrderSpecifier<?>[]{BOARD.regTime.desc()};
        }
        return pageable.getSort().stream()
                .map(this::toOrderSpecifier)
                .toArray(OrderSpecifier[]::new);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private OrderSpecifier<?> toOrderSpecifier(Sort.Order order) {
        Order direction = order.isAscending() ? Order.ASC : Order.DESC;
        return new OrderSpecifier(direction, BOARD_PATH.getComparable(order.getProperty(), Comparable.class));
    }

    private NumberExpression<Long> totalViews() {
        return Expressions.numberTemplate(Long.class, "coalesce({0}, 0)", BOARD_STATS.totalViews);
    }

    private NumberExpression<Long> totalLikes() {
        return Expressions.numberTemplate(Long.class, "coalesce({0}, 0)", BOARD_STATS.totalLikes);
    }

    private NumberExpression<Long> totalComments() {
        return Expressions.numberTemplate(Long.class, "coalesce({0}, 0)", BOARD_STATS.totalComments);
    }
}
