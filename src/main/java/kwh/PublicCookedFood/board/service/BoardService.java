package kwh.PublicCookedFood.board.service;

import jakarta.transaction.Transactional;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardSection;
import kwh.PublicCookedFood.board.domain.SoftDeleteState;
import kwh.PublicCookedFood.board.dto.request.BoardSaveRequest;
import kwh.PublicCookedFood.board.dto.response.BoardDetailResponse;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import kwh.PublicCookedFood.board.repository.CommentsRepository;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.repository.UserRepository;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BoardService {

    private static final Safelist BOARD_CONTENT_SAFELIST = Safelist.relaxed()
            .addProtocols("img", "src", "http", "https", "data");

    private final BoardRepository boardRepository;
    private final CommentsRepository commentsRepository;
    private final ImageService imageService;
    private final BoardSectionService boardSectionService;
    private final BoardPolicyService boardPolicyService;
    private final UserRepository userRepository;

    public Page<Board> getBoardList(Pageable pageable, String sectionKey) {
        return boardRepository.findAllByStateWithUser(SoftDeleteState.ACTIVE, sectionKey, pageable);
    }

    public Page<Board> getFeaturedBoardList(Pageable pageable, String sectionKey) {
        long threshold = boardPolicyService.getFeaturedLikeThreshold();
        return boardRepository.findFeaturedByStateWithUser(SoftDeleteState.ACTIVE, threshold, sectionKey, pageable);
    }

    public BoardDetailResponse getBoardDetail(Long id){
        Board board = boardRepository.findByIdWithUser(id).orElseThrow();
        return toBoardDetailResponse(board);
    }

    @Transactional
    public Page<Board> findByKeyword(String search, String sectionKey, Pageable pageable) {
        return boardRepository.findByTitleContainingAndStateWithUser(search, SoftDeleteState.ACTIVE, sectionKey, pageable);
    }

    @Transactional
    public Page<Board> findFeaturedByKeyword(String search, String sectionKey, Pageable pageable) {
        long threshold = boardPolicyService.getFeaturedLikeThreshold();
        return boardRepository.findFeaturedByTitleContainingAndStateWithUser(search, SoftDeleteState.ACTIVE, threshold, sectionKey, pageable);
    }

    @Transactional
    public Board save(BoardSaveRequest boardDto) {
        String rawContents = boardDto.getContents();
        BoardSection section = boardSectionService.resolveSectionForWrite(boardDto.getSectionId());
        Users user = userRepository.findById(boardDto.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid user ID"));
        boardDto.setTitle(Jsoup.clean(boardDto.getTitle() == null ? "" : boardDto.getTitle(), Safelist.none()));
        boardDto.setContents(Jsoup.clean(rawContents == null ? "" : rawContents, BOARD_CONTENT_SAFELIST));
        Board board = Board.builder()
                .id(boardDto.getId())
                .title(boardDto.getTitle())
                .contents(boardDto.getContents())
                .user(user)
                .section(section)
                .views(boardDto.getViews())
                .likeCount(boardDto.getLikesCount())
                .commentCount(boardDto.getCommentsCount())
                .state(boardDto.getState() == null ? SoftDeleteState.ACTIVE : boardDto.getState())
                .build();
        Board savedBoard = boardRepository.save(board);
        imageService.syncBoardImages(savedBoard, rawContents);
        return savedBoard;
    }

    @Transactional
    public void updateViews(Long id) {
        boardRepository.updateViews(id);
    }

    @Transactional
    public void delete(Long id) {
        Board board = boardRepository.findById(id).orElseThrow();
        imageService.deleteBoardImages(board);
        boardRepository.updateState(id, SoftDeleteState.DELETED);
    }

    @Transactional
    public void updateLikes(Long id) {
        boardRepository.updateLikes(id);
    }

    @Transactional
    public void updateCommentCounts(Long id) {
        Long commentCount = commentsRepository.countByBoardIdAndState(id, SoftDeleteState.ACTIVE);
        boardRepository.updateCommentCount(id, commentCount, SoftDeleteState.ACTIVE);
    }

    public int getFeaturedLikeThreshold() {
        return boardPolicyService.getFeaturedLikeThreshold();
    }

    private BoardDetailResponse toBoardDetailResponse(Board board) {
        return BoardDetailResponse.builder()
                .id(board.getId())
                .title(board.getTitle())
                .contents(board.getContents())
                .userId(board.getUser() == null ? null : board.getUser().getId())
                .userName(board.getUser() == null ? null : board.getUser().getName())
                .sectionId(board.getSection() == null ? null : board.getSection().getId())
                .sectionKey(board.getSection() == null ? null : board.getSection().getSectionKey())
                .sectionName(board.getSection() == null ? null : board.getSection().getSectionName())
                .views(board.getViews())
                .likesCount(board.getLikeCount())
                .commentsCount(board.getCommentCount())
                .state(board.getState())
                .regTime(board.getRegTime())
                .updateTime(board.getUpdateTime())
                .build();
    }
}
