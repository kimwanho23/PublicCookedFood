package kwh.PublicCookedFood.board.service;

import jakarta.transaction.Transactional;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.dto.BoardDto;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import kwh.PublicCookedFood.board.repository.CommentsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final CommentsRepository commentsRepository;

    public Page<Board> getBoardList(Pageable pageable) {
        return boardRepository.findAllByStateOrderByRegTimeDesc("1", pageable);
    }

    public BoardDto getBoardDetail(Long id){
        return boardRepository.findById(id).orElseThrow().toResponseDto();
    }

    @Transactional
    public Page<Board> findByKeyword(String search, Pageable pageable) {
        return boardRepository.findByTitleContainingAndStateOrderByRegTimeDesc(search, "1", pageable);
    }

    @Transactional
    public Board save(BoardDto boardDto) {
        return boardRepository.save(boardDto.toEntity());
    }

    @Transactional
    public void updateViews(Long id) {
        boardRepository.updateViews(id);
    }

    @Transactional
    public void delete(Long id) {
        boardRepository.deleteBoard(id);
    }

    @Transactional
    public void updateLikes(Long id) {
        boardRepository.updateLikes(id);
    }
    @Transactional
    public void updateCommentCounts(Long id) {
        Long commentCount = commentsRepository.countByBoardIdAndState(id, "1");
        boardRepository.updateCommentCount(id, commentCount);
    }
}
