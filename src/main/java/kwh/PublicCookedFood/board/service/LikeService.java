package kwh.PublicCookedFood.board.service;

import jakarta.transaction.Transactional;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.Likes;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import kwh.PublicCookedFood.board.repository.LikesRepository;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikesRepository likesRepository;

    private final UserRepository userRepository;

    private final BoardRepository boardRepository;

    @Transactional
    public Long saveLikes(Long boardId, Long userId) {
        if (likesRepository.existsByBoardIdAndUserId(boardId, userId)) {
            return null;
        }

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user ID"));
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid board ID"));

        Likes like = Likes.builder()
                .board(board)
                .user(user)
                .build();
        return likesRepository.save(like).getId();
    }

    public Long getLike(Long id){
        return likesRepository.countByBoardId(id);
    }

    public Boolean findMyLike(Long boardId, Long userId){
        return likesRepository.existsByBoardIdAndUserId(boardId, userId);
    }
}
