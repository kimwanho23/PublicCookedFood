package kwh.PublicCookedFood.board.policy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;

class BoardViewPolicyUnitTest {

    private final BoardViewPolicy boardViewPolicy = new BoardViewPolicy();

    @Test
    void shouldIncreaseDetailView_returnsFalseOnceAfterSkipWasMarked() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(new MockHttpSession());
        boardViewPolicy.markSkipNextDetailView(request, 10L);

        boolean firstResult = boardViewPolicy.shouldIncreaseDetailView(request, 10L);
        boolean secondResult = boardViewPolicy.shouldIncreaseDetailView(request, 10L);

        assertThat(firstResult).isFalse();
        assertThat(secondResult).isTrue();
    }

    @Test
    void shouldIncreaseDetailView_doesNotAffectDifferentBoardId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(new MockHttpSession());
        boardViewPolicy.markSkipNextDetailView(request, 10L);

        boolean result = boardViewPolicy.shouldIncreaseDetailView(request, 11L);

        assertThat(result).isTrue();
    }

    @Test
    void markSkipNextDetailView_createsSessionWhenMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        boardViewPolicy.markSkipNextDetailView(request, 10L);

        assertThat(request.getSession(false)).isNotNull();
        assertThat(boardViewPolicy.shouldIncreaseDetailView(request, 10L)).isFalse();
    }
}
