package kwh.PublicCookedFood.board.service;

import org.springframework.transaction.annotation.Transactional;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.Likes;
import kwh.PublicCookedFood.board.domain.SoftDeleteState;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import kwh.PublicCookedFood.board.repository.LikesRepository;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.repository.UserRepository;
import kwh.PublicCookedFood.user.service.UserBlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikesRepository likesRepository;

    private final UserRepository userRepository;

    private final BoardRepository boardRepository;

    private final UserBlockService userBlockService;

    @Transactional
    public Long saveLikes(Long boardId, Long userId) {
        if (likesRepository.existsByBoardIdAndUserId(boardId, userId)) {
            return null;
        }

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user ID"));
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid board ID"));
        if (board.getState() != SoftDeleteState.ACTIVE) {
            throw new IllegalArgumentException("삭제된 게시글에는 좋아요를 누를 수 없습니다.");
        }
        if (board.getUser() != null && userBlockService.isEitherBlocked(userId, board.getUser().getId())) {
            throw new IllegalStateException("차단 관계인 사용자의 게시글에는 좋아요를 누를 수 없습니다.");
        }

        Likes like = Likes.builder()
                .board(board)
                .user(user)
                .build();
        try {
            return likesRepository.saveAndFlush(like).getId();
        } catch (DataIntegrityViolationException e) {
            // 동시 요청 경합으로 unique 제약 충돌 시 기존 레코드를 재조회한다.
            return likesRepository.findByBoardIdAndUserId(boardId, userId)
                    .map(Likes::getId)
                    .orElse(null);
        }
    }

    public Long getLike(Long id){
        return likesRepository.countByBoardId(id);
    }

    public Boolean findMyLike(Long boardId, Long userId){
        return likesRepository.existsByBoardIdAndUserId(boardId, userId);
    }
}
