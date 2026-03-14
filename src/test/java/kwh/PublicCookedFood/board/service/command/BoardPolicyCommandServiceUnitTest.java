package kwh.PublicCookedFood.board.service.command;

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
class BoardPolicyCommandServiceUnitTest {

    @Mock
    private BoardPolicyRepository boardPolicyRepository;

    private BoardPolicyCommandService boardPolicyCommandService;

    @BeforeEach
    void setUp() {
        boardPolicyCommandService = new BoardPolicyCommandService(boardPolicyRepository);
    }

    @Test
    void updateFeaturedLikeThreshold_createsDefaultPolicyWhenMissing() {
        BoardPolicy savedPolicy = BoardPolicy.createDefault();
        when(boardPolicyRepository.findTopByOrderByIdAsc()).thenReturn(Optional.empty());
        when(boardPolicyRepository.save(org.mockito.ArgumentMatchers.any(BoardPolicy.class))).thenReturn(savedPolicy);

        BoardPolicy policy = boardPolicyCommandService.updateFeaturedLikeThreshold(7);

        assertThat(policy.getFeaturedLikeThreshold()).isEqualTo(7);
        verify(boardPolicyRepository).save(org.mockito.ArgumentMatchers.any(BoardPolicy.class));
    }

    @Test
    void updateThumbnailDisplayMode_updatesExistingPolicy() {
        BoardPolicy policy = BoardPolicy.createDefault();
        when(boardPolicyRepository.findTopByOrderByIdAsc()).thenReturn(Optional.of(policy));

        BoardPolicy updated = boardPolicyCommandService.updateThumbnailDisplayMode(BoardThumbnailDisplayMode.HOVER);

        assertThat(updated.getThumbnailDisplayMode()).isEqualTo(BoardThumbnailDisplayMode.HOVER);
    }
}
