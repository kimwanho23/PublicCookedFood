package kwh.PublicCookedFood.board.facade;

import kwh.PublicCookedFood.board.application.query.view.BoardDetailPageView;
import kwh.PublicCookedFood.board.application.query.view.CommentNodeView;
import kwh.PublicCookedFood.board.domain.BoardReportReason;
import kwh.PublicCookedFood.board.dto.response.BoardDetailResponse;
import kwh.PublicCookedFood.board.service.BoardCounters;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BoardDetailReadModelFactoryUnitTest {

    private final BoardDetailReadModelFactory boardDetailReadModelFactory = new BoardDetailReadModelFactory();

    @Test
    void toViewData_buildsStrictReadModelWithReportReasonsCopy() {
        Page<CommentNodeView> comments = Page.empty(PageRequest.of(0, 20));

        BoardDetailPageView viewData = boardDetailReadModelFactory.toViewData(
                boardDetailReadModelFactory.applyCounters(
                        BoardDetailResponse.builder().id(10L).accountId(1L).version(4L).build(),
                        new BoardCounters(3L, 0L, 0L)
                ),
                new BoardCounters(3L, 0L, 0L),
                0L,
                BoardInteractionState.anonymous(),
                comments
        );

        assertThatThrownBy(() -> viewData.reportReasons().set(0, null))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(viewData.board().getId()).isEqualTo(10L);
        assertThat(viewData.board().getViews()).isEqualTo(3L);
        assertThat(viewData.board().getVersion()).isEqualTo(4L);
        assertThat(viewData.counters().likes()).isEqualTo(0L);
        assertThat(viewData.commentThread().boardId()).isEqualTo(10L);
        assertThat(viewData.commentThread().boardInteractionBlocked()).isFalse();
        assertThat(viewData.comments()).isSameAs(comments);
        assertThat(viewData.reportReasons().get(0)).isEqualTo(BoardReportReason.values()[0]);
    }

    @Test
    void toViewData_rejectsNullInteractionState() {
        assertThatThrownBy(() -> boardDetailReadModelFactory.toViewData(
                boardDetailReadModelFactory.applyCounters(
                        BoardDetailResponse.builder().id(10L).accountId(1L).build(),
                        new BoardCounters(3L, 0L, 0L)
                ),
                new BoardCounters(3L, 0L, 0L),
                0L,
                null,
                Page.empty(PageRequest.of(0, 20))
        )).isInstanceOf(NullPointerException.class)
                .hasMessage("interactionState");
    }
}
