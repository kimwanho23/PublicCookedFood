package kwh.PublicCookedFood.board.controller.support;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.board.application.query.view.BoardEditFormView;
import kwh.PublicCookedFood.board.domain.BoardSection;
import kwh.PublicCookedFood.board.dto.request.BoardUpdateRequest;
import kwh.PublicCookedFood.board.dto.request.BoardWriteRequest;
import kwh.PublicCookedFood.board.facade.BoardViewer;
import kwh.PublicCookedFood.board.policy.BoardAuthorizationPolicy;
import kwh.PublicCookedFood.board.service.command.BoardSectionCommandService;
import kwh.PublicCookedFood.board.service.query.BoardDetailQueryService;
import kwh.PublicCookedFood.board.service.query.BoardSectionQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BoardWriteFormSupport {

    private final BoardDetailQueryService boardDetailQueryService;
    private final BoardSectionQueryService boardSectionQueryService;
    private final BoardSectionCommandService boardSectionCommandService;
    private final BoardAuthorizationPolicy boardAuthorizationPolicy;

    public List<BoardSection> loadActiveSections() {
        return boardSectionQueryService.getActiveSections();
    }

    public BoardWriteRequest prepareBoardWriteRequest(BoardWriteRequest source) {
        return BoardWriteRequest.prepared(source, defaultSectionId());
    }

    public BoardUpdateRequest prepareBoardUpdateRequest(Long boardId, BoardUpdateRequest source) {
        return BoardUpdateRequest.prepared(boardId, source, defaultSectionId());
    }

    public BoardManageContext loadBoardManageContext(Account actor, Long boardId) {
        BoardEditFormView board = boardDetailQueryService.getBoardEditForm(boardId);
        boolean manageable = boardAuthorizationPolicy.canManageBoard(BoardViewer.from(actor), board.authorAccountId());
        return new BoardManageContext(board, manageable);
    }

    public BoardUpdateRequest toBoardUpdateRequest(BoardEditFormView boardDetail) {
        Long resolvedSectionId = boardDetail.sectionId() != null
                ? boardDetail.sectionId()
                : defaultSectionId();
        return BoardUpdateRequest.builder()
                .id(boardDetail.id())
                .version(boardDetail.version())
                .title(boardDetail.title())
                .contents(boardDetail.contents())
                .sectionId(resolvedSectionId)
                .build();
    }

    private Long defaultSectionId() {
        return boardSectionCommandService.ensureDefaultSection().getId();
    }

    public record BoardManageContext(BoardEditFormView board, boolean manageable) {
    }
}
