package kwh.PublicCookedFood.board.service.query;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import kwh.PublicCookedFood.board.dto.response.BoardDetailResponse;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardDetailQueryServiceUnitTest {

    @Mock
    private BoardRepository boardRepository;

    private BoardDetailQueryService boardDetailQueryService;

    @BeforeEach
    void setUp() {
        boardDetailQueryService = new BoardDetailQueryService(boardRepository);
    }

    @Test
    void getBoardDetail_loadsActiveBoardAndMapsResponse() {
        Board board = Board.builder()
                .id(10L)
                .title("title")
                .contents("contents")
                .version(5L)
                .state(SoftDeleteState.ACTIVE)
                .hiddenByReport(false)
                .build();
        when(boardRepository.findByIdWithAccountAndState(10L, SoftDeleteState.ACTIVE)).thenReturn(Optional.of(board));

        BoardDetailResponse result = boardDetailQueryService.getBoardDetail(10L);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getTitle()).isEqualTo("title");
        assertThat(result.getViews()).isNull();
        assertThat(result.getVersion()).isEqualTo(5L);
    }
}
