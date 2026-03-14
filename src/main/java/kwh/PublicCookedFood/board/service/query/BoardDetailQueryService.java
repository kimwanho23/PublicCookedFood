package kwh.PublicCookedFood.board.service.query;

import kwh.PublicCookedFood.board.application.query.view.BoardEditFormView;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import kwh.PublicCookedFood.board.dto.response.BoardDetailResponse;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class BoardDetailQueryService {

    private final BoardRepository boardRepository;

    public BoardDetailResponse getBoardDetail(Long boardId) {
        return BoardDetailResponse.from(findActiveBoard(boardId));
    }

    public BoardEditFormView getBoardEditForm(Long boardId) {
        return BoardEditFormView.from(findActiveBoard(boardId));
    }

    private Board findActiveBoard(Long boardId) {
        return boardRepository.findByIdWithAccountAndState(boardId, SoftDeleteState.ACTIVE)
                .orElseThrow(() -> new NoSuchElementException("유효한 게시글을 찾을 수 없습니다."));
    }
}
