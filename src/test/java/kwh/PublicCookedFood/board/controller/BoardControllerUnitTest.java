package kwh.PublicCookedFood.board.controller;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.board.controller.support.BoardWriteFormSupport;
import kwh.PublicCookedFood.board.controller.support.BoardViewerSupport;
import kwh.PublicCookedFood.board.dto.request.BoardUpdateRequest;
import kwh.PublicCookedFood.board.facade.BoardListQueryFacade;
import kwh.PublicCookedFood.board.facade.BoardScrapQueryFacade;
import kwh.PublicCookedFood.board.service.command.BoardCommandService;
import kwh.PublicCookedFood.board.service.command.BoardUpdateCommand;
import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.common.error.CommonErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardControllerUnitTest {

    @Mock
    private BoardListQueryFacade boardListQueryFacade;

    @Mock
    private BoardScrapQueryFacade boardScrapQueryFacade;

    @Mock
    private BoardWriteFormSupport boardWriteFormSupport;

    @Mock
    private BoardCommandService boardCommandService;

    private BoardController boardController;

    @BeforeEach
    void setUp() {
        boardController = new BoardController(
                boardListQueryFacade,
                boardScrapQueryFacade,
                boardWriteFormSupport,
                boardCommandService,
                new BoardViewerSupport()
        );
    }

    @Test
    void updateBoard_rendersEditFormWhenVersionConflictOccurs() {
        Account account = account(100L);
        BoardUpdateRequest request = BoardUpdateRequest.builder()
                .version(3L)
                .title("제목")
                .contents("내용")
                .sectionId(7L)
                .build();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(request, "boardDto");
        Model model = new ExtendedModelMap();

        when(boardWriteFormSupport.loadBoardManageContext(account, 10L))
                .thenReturn(new BoardWriteFormSupport.BoardManageContext(null, true));
        when(boardWriteFormSupport.prepareBoardUpdateRequest(10L, request)).thenReturn(request);
        when(boardWriteFormSupport.loadActiveSections()).thenReturn(List.of());
        doThrow(new AppException(CommonErrorCode.REQUEST_CONFLICT, "다른 사용자가 이미 게시글을 수정했습니다."))
                .when(boardCommandService)
                .update(BoardUpdateCommand.of(100L, 10L, 3L, "제목", "내용", 7L));

        String viewName = boardController.updateBoard(account, 10L, request, bindingResult, model);

        verify(boardCommandService).update(BoardUpdateCommand.of(100L, 10L, 3L, "제목", "내용", 7L));
        assertThat(viewName).isEqualTo("boards/updateBoard");
        assertThat(model.getAttribute("errorMessage")).isEqualTo("다른 사용자가 이미 게시글을 수정했습니다.");
        assertThat(model.getAttribute("boardDto")).isEqualTo(request);
    }

    private Account account(Long accountId) {
        return Account.builder()
                .id(accountId)
                .email("writer@test.com")
                .name("writer")
                .authority(Role.USER)
                .loginMethod("Current")
                .build();
    }
}
