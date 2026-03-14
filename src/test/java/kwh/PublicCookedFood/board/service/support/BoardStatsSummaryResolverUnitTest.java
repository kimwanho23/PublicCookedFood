package kwh.PublicCookedFood.board.service.support;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.repository.BoardStatsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardStatsSummaryResolverUnitTest {

    @Mock
    private BoardStatsRepository boardStatsRepository;

    @InjectMocks
    private BoardStatsSummaryResolver boardStatsSummaryResolver;

    @Test
    void resolve_prefersBoardStatsAndDefaultsMissingStatsToZero() {
        Board first = Board.builder()
                .id(10L)
                .build();
        Board second = Board.builder()
                .id(11L)
                .build();

        when(boardStatsRepository.findCounterSummariesByBoardIdIn(anyCollection()))
                .thenReturn(List.of(new Projection(10L, 100L, 200L, 300L)));

        var summaries = boardStatsSummaryResolver.resolve(List.of(first, second));

        assertThat(summaries.get(10L)).isEqualTo(new BoardStatsSummary(100L, 200L, 300L));
        assertThat(summaries.get(11L)).isEqualTo(BoardStatsSummary.ZERO);
    }

    private static final class Projection implements BoardStatsRepository.BoardStatsSummaryProjection {
        private final Long boardId;
        private final Long totalViews;
        private final Long totalLikes;
        private final Long totalComments;

        private Projection(Long boardId, Long totalViews, Long totalLikes, Long totalComments) {
            this.boardId = boardId;
            this.totalViews = totalViews;
            this.totalLikes = totalLikes;
            this.totalComments = totalComments;
        }

        @Override
        public Long getBoardId() {
            return boardId;
        }

        @Override
        public Long getTotalViews() {
            return totalViews;
        }

        @Override
        public Long getTotalLikes() {
            return totalLikes;
        }

        @Override
        public Long getTotalComments() {
            return totalComments;
        }
    }
}
