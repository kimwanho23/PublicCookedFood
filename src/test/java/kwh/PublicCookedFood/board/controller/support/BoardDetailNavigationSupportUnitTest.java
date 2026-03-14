package kwh.PublicCookedFood.board.controller.support;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class BoardDetailNavigationSupportUnitTest {

    private final BoardDetailNavigationSupport boardDetailNavigationSupport = new BoardDetailNavigationSupport();

    @Test
    void isCommentPageNavigation_returnsTrueForCommentPagingMoveWithinSameBoard() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/boards/10");
        request.setServerName("localhost");
        request.addHeader("Referer", "http://localhost/boards/10?commentPage=0&commentSize=50");
        request.addParameter("commentPage", "1");
        request.addParameter("commentSize", "50");

        boolean commentPageNavigation = boardDetailNavigationSupport.isCommentPageNavigation(request, 10L);

        assertThat(commentPageNavigation).isTrue();
    }

    @Test
    void commentPageRedirect_normalizesPageAndSize() {
        String redirect = boardDetailNavigationSupport.commentPageRedirect(10L, -1, 999);

        assertThat(redirect).isEqualTo("redirect:/boards/10?commentPage=0&commentSize=100#board-comments");
    }

    @Test
    void resolveCommentPageable_appliesAscendingRegTimeSort() {
        Pageable pageable = boardDetailNavigationSupport.resolveCommentPageable(1, 50);

        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(50);
        assertThat(pageable.getSort().getOrderFor("regTime")).isNotNull();
    }

    @Test
    void isCommentPageNavigation_returnsFalseWhenRefererIsDifferentBoard() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/boards/10");
        request.setServerName("localhost");
        request.addHeader("Referer", "http://localhost/boards/9?commentPage=0&commentSize=50");
        request.addParameter("commentPage", "1");
        request.addParameter("commentSize", "50");

        boolean commentPageNavigation = boardDetailNavigationSupport.isCommentPageNavigation(request, 10L);

        assertThat(commentPageNavigation).isFalse();
    }
}
