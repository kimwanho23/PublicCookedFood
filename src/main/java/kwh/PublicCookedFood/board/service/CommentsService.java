package kwh.PublicCookedFood.board.service;

import jakarta.transaction.Transactional;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.Comments;
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
        commentsRepository.deleteCommentOption(id);
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

    public Comments getComment(Long id) {
        return commentsRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid comment ID"));
    }


    public Page<CommentsDto> getCommentListWithReplies(Long postId, Pageable pageable) {
        List<Comments> allComments = commentsRepository.findAllByBoardIdWithUserAndParentOrderByRegTimeAsc(postId);

        List<Comments> parentComments = allComments.stream()
                .filter(comment -> comment.getParent() == null)
                .toList();

        Map<Long, List<Comments>> repliesByParentId = allComments.stream()
                .filter(comment -> comment.getParent() != null)
                .collect(Collectors.groupingBy(comment -> comment.getParent().getId(),
                        LinkedHashMap::new, Collectors.toList()));

        int start = (int) pageable.getOffset();
        if (start >= parentComments.size()) {
            return new PageImpl<>(List.of(), pageable, parentComments.size());
        }
        int end = Math.min(start + pageable.getPageSize(), parentComments.size());

        List<CommentsDto> content = parentComments.subList(start, end).stream()
                .map(comment -> convertToDto(comment, repliesByParentId))
                .toList();

        return new PageImpl<>(content, pageable, parentComments.size());
    }


    private CommentsDto convertToDto(Comments comment) {
        return convertToDto(comment, Map.of());
    }

    private CommentsDto convertToDto(Comments comment, Map<Long, List<Comments>> repliesByParentId) {
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

        List<CommentsDto> replies = repliesByParentId.getOrDefault(comment.getId(), List.of())
                .stream()
                .map(reply -> convertToDto(reply, repliesByParentId))
                .toList();
        dto.setReplies(replies);

        return dto;
    }

}
