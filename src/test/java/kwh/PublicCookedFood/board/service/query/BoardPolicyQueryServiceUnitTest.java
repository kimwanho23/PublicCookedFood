package kwh.PublicCookedFood.board.service.query;

import kwh.PublicCookedFood.board.domain.BoardPolicy;
import kwh.PublicCookedFood.board.domain.BoardThumbnailDisplayMode;
import kwh.PublicCookedFood.board.repository.BoardPolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardPolicyQueryServiceUnitTest {

    @Mock
    private BoardPolicyRepository boardPolicyRepository;

    private BoardPolicyQueryService boardPolicyQueryService;

    @BeforeEach
    void setUp() {
        boardPolicyQueryService = new BoardPolicyQueryService(boardPolicyRepository);
    }

    @Test
    void getPolicy_returnsDefaultPolicyWhenRepositoryIsEmpty() {
        when(boardPolicyRepository.findTopByOrderByIdAsc()).thenReturn(Optional.empty());

        BoardPolicy policy = boardPolicyQueryService.getPolicy();

        assertThat(policy.getFeaturedLikeThreshold()).isEqualTo(10);
        assertThat(policy.getThumbnailDisplayMode()).isEqualTo(BoardThumbnailDisplayMode.LEFT);
        verify(boardPolicyRepository).findTopByOrderByIdAsc();
    }
}
