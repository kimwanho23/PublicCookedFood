package kwh.PublicCookedFood.board.facade;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardSection;
import kwh.PublicCookedFood.board.dto.request.BoardUpdateRequest;
import kwh.PublicCookedFood.board.dto.request.BoardWriteRequest;
import kwh.PublicCookedFood.board.dto.response.BoardDetailResponse;
import kwh.PublicCookedFood.board.policy.BoardAuthorizationPolicy;
import kwh.PublicCookedFood.board.service.BoardScrapService;
import kwh.PublicCookedFood.board.service.BoardSectionService;
import kwh.PublicCookedFood.board.service.BoardService;
import kwh.PublicCookedFood.common.dto.request.BoardSearchQuery;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.service.UserBlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BoardFacade {

    private final BoardService boardService;
    private final BoardSectionService boardSectionService;
    private final BoardScrapService boardScrapService;
    private final UserBlockService userBlockService;
    private final BoardAuthorizationPolicy boardAuthorizationPolicy;
    private final BoardListQueryFacade boardListQueryFacade;
    private final BoardCommandFacade boardCommandFacade;

    public BoardListViewData loadBoardList(Pageable pageable,
                                           BoardSearchQuery query,
                                           boolean featuredPage,
                                           boolean hasBindingErrors,
                                           Long viewerUserId) {
        return boardListQueryFacade.loadBoardList(
                pageable,
                query,
                featuredPage,
                hasBindingErrors,
                viewerUserId
        );
    }

    public List<Board> loadMyScrappedBoards(Long userId) {
        Set<Long> blockedUserIds = userBlockService.getViewRestrictedUserIds(userId);
        return boardScrapService.getMyScrappedBoards(userId, blockedUserIds);
    }

    public List<BoardSection> loadActiveSections() {
        return boardSectionService.getActiveSections();
    }

    public void applyDefaultSection(BoardWriteRequest boardDto) {
        if (boardDto.getSectionId() == null) {
            boardDto.setSectionId(boardSectionService.ensureDefaultSection().getId());
        }
    }

    public void applyDefaultSection(BoardUpdateRequest boardDto) {
        if (boardDto.getSectionId() == null) {
            boardDto.setSectionId(boardSectionService.ensureDefaultSection().getId());
        }
    }

    public BoardManageContext loadBoardManageContext(Users actor, Long boardId) {
        BoardDetailResponse board = boardService.getBoardDetail(boardId);
        boolean manageable = boardAuthorizationPolicy.canManageBoard(actor, board);
        return new BoardManageContext(board, manageable);
    }

    public BoardUpdateRequest toBoardUpdateRequest(BoardDetailResponse boardDetail) {
        return BoardUpdateRequest.from(
                boardDetail,
                boardSectionService.ensureDefaultSection().getId()
        );
    }

    public Board createBoard(Long actorUserId, BoardWriteRequest boardDto) {
        return boardCommandFacade.createBoard(actorUserId, boardDto);
    }

    public void updateBoard(Long actorUserId,
                            Long boardId,
                            BoardUpdateRequest boardDto,
                            BoardDetailResponse existingBoard) {
        boardCommandFacade.updateBoard(actorUserId, boardId, boardDto, existingBoard);
    }

    public void deleteBoard(Long actorUserId, Long boardId) {
        boardCommandFacade.deleteBoard(actorUserId, boardId);
    }

    public record BoardListViewData(Page<Board> boardList,
                                    Pageable listPageable,
                                    String search,
                                    String activeSection,
                                    String orderBy,
                                    Map<Long, String> boardThumbnailMap,
                                    Map<Long, Boolean> boardHasImageMap,
                                    String thumbnailDisplayMode,
                                    List<BoardSection> sections,
                                    String boardListPath,
                                    boolean featuredPage,
                                    int featuredLikeThreshold,
                                    String boardPageTitle,
                                    String queryErrorMsg) {
        public BoardListViewData {
            boardThumbnailMap = boardThumbnailMap == null ? Map.of() : Map.copyOf(boardThumbnailMap);
            boardHasImageMap = boardHasImageMap == null ? Map.of() : Map.copyOf(boardHasImageMap);
            sections = sections == null ? List.of() : List.copyOf(sections);
        }
    }

    public record BoardManageContext(BoardDetailResponse board,
                                     boolean manageable) {
    }

}
