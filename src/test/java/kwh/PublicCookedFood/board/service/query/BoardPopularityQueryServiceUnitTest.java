package kwh.PublicCookedFood.board.service.query;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardPopularityQueryServiceUnitTest {

    @Mock
    private BoardRepository boardRepository;

    private BoardPopularityQueryService boardPopularityQueryService;

    @BeforeEach
    void setUp() {
        boardPopularityQueryService = new BoardPopularityQueryService(boardRepository);
    }

    @Test
    void getPopularBoardsSince_normalizesLimitAndBaseline() {
        LocalDateTime since = LocalDateTime.of(2026, 3, 1, 12, 0);
        List<Board> boards = List.of(Board.builder().id(3L).state(SoftDeleteState.ACTIVE).hiddenByReport(false).build());
        when(boardRepository.findTopByStateAndRegTimeAfterOrderByPopularity(
                SoftDeleteState.ACTIVE,
                since,
                PageRequest.of(0, 30)
        )).thenReturn(boards);

        List<Board> result = boardPopularityQueryService.getPopularBoardsSince(since, 99);

        assertThat(result).isEqualTo(boards);
        verify(boardRepository).findTopByStateAndRegTimeAfterOrderByPopularity(
                SoftDeleteState.ACTIVE,
                since,
                PageRequest.of(0, 30)
        );
    }
}
