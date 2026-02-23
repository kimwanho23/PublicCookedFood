package kwh.PublicCookedFood.board.service;

import jakarta.transaction.Transactional;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.board.domain.SoftDeleteState;
import kwh.PublicCookedFood.board.dto.request.CommentCreateRequest;
import kwh.PublicCookedFood.board.dto.response.CommentResponse;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentsService {

    private final CommentsRepository commentsRepository;

    private final BoardRepository boardRepository;

    private final UserRepository userRepository;


    @Transactional
    public void deleteComment(Long id) {
        commentsRepository.updateState(id, SoftDeleteState.DELETED);
    }

    @Transactional
    public CommentResponse createComment(CommentCreateRequest commentsDto) {
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
                .state(commentsDto.getState() == null ? SoftDeleteState.ACTIVE : commentsDto.getState())
                .replies(new ArrayList<>()) // 대댓글 초기화
                .build();

        Comments savedComment = commentsRepository.save(comment);
        return convertToDto(savedComment);
    }

    public Long getCommentsCount(Long id){
        return commentsRepository.countByBoardIdAndState(id, SoftDeleteState.ACTIVE);
    }

    public Comments getComment(Long id) {
        return commentsRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid comment ID"));
    }


    public Page<CommentResponse> getCommentListWithReplies(Long postId, Pageable pageable) {
        Page<Comments> parentComments = commentsRepository
                .findParentCommentsWithUserByBoardIdOrderByRegTimeAsc(postId, pageable);

        List<Comments> replies = commentsRepository
                .findRepliesWithUserAndParentByBoardIdOrderByRegTimeAsc(postId);

        Map<Long, List<Comments>> repliesByParentId = replies.stream()
                .collect(Collectors.groupingBy(comment -> comment.getParent().getId(),
                        LinkedHashMap::new, Collectors.toList()));

        List<CommentResponse> content = parentComments.getContent().stream()
                .map(comment -> convertToDto(comment, repliesByParentId, postId))
                .toList();

        return new PageImpl<>(content, pageable, parentComments.getTotalElements());
    }


    private CommentResponse convertToDto(Comments comment) {
        return convertToDto(comment, Map.of());
    }

    private CommentResponse convertToDto(Comments comment, Map<Long, List<Comments>> repliesByParentId) {
        return convertToDto(comment, repliesByParentId, comment.getBoard().getId());
    }

    private CommentResponse convertToDto(Comments comment, Map<Long, List<Comments>> repliesByParentId, Long boardId) {
        CommentResponse dto = new CommentResponse();
        dto.setId(comment.getId());
        dto.setUserId(comment.getUser().getId());
        dto.setName(comment.getUser().getName());
        dto.setBoardId(boardId);
        dto.setContents(comment.getContents());
        dto.setParentId(comment.getParent() != null ? comment.getParent().getId() : null);
        dto.setState(comment.getState());
        dto.setRegTime(comment.getRegTime());
        dto.setUpdateTime(comment.getUpdateTime());

        List<CommentResponse> replies = repliesByParentId.getOrDefault(comment.getId(), List.of())
                .stream()
                .map(reply -> convertToDto(reply, repliesByParentId, boardId))
                .toList();
        dto.setReplies(replies);

        return dto;
    }

}
