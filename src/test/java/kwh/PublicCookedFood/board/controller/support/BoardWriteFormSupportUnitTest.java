package kwh.PublicCookedFood.board.controller.support;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.board.application.query.view.BoardEditFormView;
import kwh.PublicCookedFood.board.domain.BoardSection;
import kwh.PublicCookedFood.board.dto.request.BoardUpdateRequest;
import kwh.PublicCookedFood.board.dto.request.BoardWriteRequest;
import kwh.PublicCookedFood.board.facade.BoardViewer;
import kwh.PublicCookedFood.board.policy.BoardAuthorizationPolicy;
import kwh.PublicCookedFood.board.service.command.BoardSectionCommandService;
import kwh.PublicCookedFood.board.service.query.BoardDetailQueryService;
import kwh.PublicCookedFood.board.service.query.BoardSectionQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardWriteFormSupportUnitTest {

    @Mock
    private BoardDetailQueryService boardDetailQueryService;

    @Mock
    private BoardSectionQueryService boardSectionQueryService;

    @Mock
    private BoardSectionCommandService boardSectionCommandService;

    @Mock
    private BoardAuthorizationPolicy boardAuthorizationPolicy;

    private BoardWriteFormSupport boardWriteFormSupport;

    @BeforeEach
    void setUp() {
        boardWriteFormSupport = new BoardWriteFormSupport(
                boardDetailQueryService,
                boardSectionQueryService,
                boardSectionCommandService,
                boardAuthorizationPolicy
        );
    }

    @Test
    void prepareBoardWriteRequest_appliesDefaultSectionWithoutMutatingSource() {
        BoardWriteRequest source = new BoardWriteRequest();
        source.setTitle("title");
        source.setContents("contents");

        when(boardSectionCommandService.ensureDefaultSection()).thenReturn(section(5L));

        BoardWriteRequest prepared = boardWriteFormSupport.prepareBoardWriteRequest(source);

        assertThat(source.getSectionId()).isNull();
        assertThat(prepared.getSectionId()).isEqualTo(5L);
        assertThat(prepared.getTitle()).isEqualTo("title");
        assertThat(prepared.getContents()).isEqualTo("contents");
    }

    @Test
    void prepareBoardUpdateRequest_copiesBoardIdAndAppliesDefaultSection() {
        BoardUpdateRequest source = new BoardUpdateRequest();
        source.setVersion(6L);
        source.setTitle("title");
        source.setContents("contents");

        when(boardSectionCommandService.ensureDefaultSection()).thenReturn(section(9L));

        BoardUpdateRequest prepared = boardWriteFormSupport.prepareBoardUpdateRequest(42L, source);

        assertThat(source.getId()).isNull();
        assertThat(source.getSectionId()).isNull();
        assertThat(prepared.getId()).isEqualTo(42L);
        assertThat(prepared.getVersion()).isEqualTo(6L);
        assertThat(prepared.getSectionId()).isEqualTo(9L);
    }

    @Test
    void toBoardUpdateRequest_mapsBoardDetailWithoutDependingOnSourceId() {
        when(boardSectionCommandService.ensureDefaultSection()).thenReturn(section(7L));

        BoardUpdateRequest prepared = boardWriteFormSupport.toBoardUpdateRequest(new BoardEditFormView(
                15L,
                7L,
                3L,
                "title",
                "contents",
                null
        ));

        assertThat(prepared.getId()).isEqualTo(15L);
        assertThat(prepared.getVersion()).isEqualTo(3L);
        assertThat(prepared.getTitle()).isEqualTo("title");
        assertThat(prepared.getContents()).isEqualTo("contents");
        assertThat(prepared.getSectionId()).isEqualTo(7L);
    }

    @Test
    void loadBoardManageContext_resolvesViewerFromActor() {
        Account actor = Account.builder()
                .id(3L)
                .email("user@test.com")
                .name("user")
                .authority(Role.USER)
                .loginMethod("Current")
                .build();
        BoardEditFormView board = new BoardEditFormView(10L, 7L, 1L, "title", "contents", 2L);

        when(boardDetailQueryService.getBoardEditForm(10L)).thenReturn(board);
        when(boardAuthorizationPolicy.canManageBoard(eq(BoardViewer.authenticated(3L)), eq(7L))).thenReturn(false);

        BoardWriteFormSupport.BoardManageContext context = boardWriteFormSupport.loadBoardManageContext(actor, 10L);

        assertThat(context.board()).isSameAs(board);
        assertThat(context.manageable()).isFalse();
        verify(boardAuthorizationPolicy).canManageBoard(BoardViewer.authenticated(3L), 7L);
    }

    private BoardSection section(Long id) {
        return BoardSection.builder()
                .id(id)
                .sectionKey("default")
                .sectionName("기본")
                .displayOrder(0)
                .active(true)
                .build();
    }
}
