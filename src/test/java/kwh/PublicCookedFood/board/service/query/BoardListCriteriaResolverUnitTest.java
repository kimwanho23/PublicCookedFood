package kwh.PublicCookedFood.board.service.query;
import kwh.PublicCookedFood.board.facade.BoardViewer;
import kwh.PublicCookedFood.account.service.AccountBlockService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardListCriteriaResolverUnitTest {

    @Mock
    private BoardSectionQueryService boardSectionQueryService;

    @Mock
    private AccountBlockService accountBlockService;

    @InjectMocks
    private BoardListCriteriaResolver boardListCriteriaResolver;

    @Test
    void resolve_returnsInvalidSectionMessageWhenSectionDoesNotExist() {
        when(boardSectionQueryService.existsActiveSection("missing")).thenReturn(false);
        when(accountBlockService.getViewRestrictedAccountIds(1L)).thenReturn(Set.of(10L));

        BoardListCriteriaResolver.Resolution resolution = boardListCriteriaResolver.resolve(
                PageRequest.of(0, 20),
                null,
                "missing",
                null,
                false,
                false,
                BoardViewer.authenticated(1L)
        );

        assertThat(resolution.queryNotice().present()).isTrue();
        assertThat(resolution.queryNotice().text()).contains(BoardListCriteriaResolver.SECTION_INVALID_MESSAGE);
        assertThat(resolution.criteria().sectionKey()).isEmpty();
        assertThat(resolution.criteria().visibility().restrictedAccountIds()).containsExactly(10L);
    }

    @Test
    void resolve_normalizesViewsOrderIntoCriteria() {
        BoardListCriteriaResolver.Resolution resolution = boardListCriteriaResolver.resolve(
                PageRequest.of(0, 15),
                null,
                null,
                "views",
                false,
                false,
                BoardViewer.anonymous()
        );

        assertThat(resolution.criteria().order()).isEqualTo(BoardListOrder.VIEWS);
        assertThat(resolution.criteria().orderByParam()).isEqualTo("views");
    }
}
