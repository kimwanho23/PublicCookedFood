package kwh.PublicCookedFood.board.service;

import jakarta.transaction.Transactional;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.board.dto.BoardDto;
import kwh.PublicCookedFood.board.dto.CommentsDto;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import kwh.PublicCookedFood.board.repository.CommentsRepository;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentsService {

    private final CommentsRepository commentsRepository;

    private final BoardRepository boardRepository;

    private final UserRepository userRepository;


    @Transactional
    public void deleteComment(Long id) {
        commentsRepository.deleteComment(id);
    }

    @Transactional
    public CommentsDto createComment(CommentsDto commentsDto) {
        Comments parent = null;
        if (commentsDto.getParentId() != null) {
            parent = commentsRepository.findById(commentsDto.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid parent comment ID"));
        }
        Users user = userRepository.findById(commentsDto.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid user ID"));

        Board board = boardRepository.findById(commentsDto.getBoardId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid board ID"));

        Comments comment = Comments.builder()
                .user(user)
                .board(board)
                .contents(commentsDto.getContents())
                .parent(parent)
                .state(commentsDto.getState() != null ? commentsDto.getState() : "1")
                .replies(new ArrayList<>()) // 대댓글 초기화
                .build();

        Comments savedComment = commentsRepository.save(comment);
        return convertToDto(savedComment);
    }

    public Long getCommentsCount(Long id){
        return commentsRepository.countByBoardIdAndState(id, "1");
    }


    public Page<CommentsDto> getCommentListWithReplies(Long postId, Pageable pageable) {
        // 최상위 댓글(부모 댓글이 없는 댓글)들을 먼저 조회
        Page<Comments> parentComments = commentsRepository.findByBoardIdAndParentIsNullOrderByRegTimeAsc(postId, pageable);

        // 각 최상위 댓글에 대해 대댓글들을 재귀적으로 조회하여 DTO로 변환
        return parentComments.
                map(this::convertToDto);
    }


    private CommentsDto convertToDto(Comments comment) {
        CommentsDto dto = new CommentsDto();
        dto.setId(comment.getId());
        dto.setUserId(comment.getUser().getId());
        dto.setName(comment.getUser().getName());
        dto.setBoardId(comment.getBoard().getId());
        dto.setContents(comment.getContents());
        dto.setParentId(comment.getParent() != null ? comment.getParent().getId() : null);
        dto.setState(comment.getState());
        dto.setRegTime(comment.getRegTime());
        dto.setUpdateTime(comment.getUpdateTime());

        // 대댓글들을 재귀적으로 처리하여 DTO로 변환
        List<CommentsDto> replies = comment.getReplies().stream()
                .map(this::convertToDto)
                .toList();
        dto.setReplies(replies);

        return dto;
    }

}
