package kwh.PublicCookedFood.board.contoller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.dto.BoardDto;
import kwh.PublicCookedFood.board.dto.CommentsDto;
import kwh.PublicCookedFood.board.service.BoardService;
import kwh.PublicCookedFood.board.service.CommentsService;
import kwh.PublicCookedFood.board.service.LikeService;
import kwh.PublicCookedFood.common.Paging;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;



@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/board")
public class BoardController {

    private final BoardService boardService;

    private final CommentsService commentsService;

    private final LikeService likeService;

    private final HttpSession httpSession;

    private final UserService userService;

    @GetMapping("")
    public String boardList(Model model, @PageableDefault(page = 0, size = 15, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
                            @RequestParam(required = false) String keyword,
                            @RequestParam(required = false) String search,
                            HttpServletRequest request) {
        Page<Board> boardList = getBoardInfo(pageable, search);
        Paging.addPagingAttributes(model, boardList, pageable); //페이징 알고리즘

        if ((keyword != null && keyword.trim().isEmpty()) || (search != null && search.trim().isEmpty())) {
            return Paging.handleEmptyParamsRedirect(request, keyword, search);
        }
        model.addAttribute("boardList", boardList);
        model.addAttribute("search", search);
        return "/boards/boardList";
    }

    private Page<Board> getBoardInfo(Pageable pageable, String search) {
        if (search == null || search.isEmpty()) {
            return boardService.getBoardList(pageable);
        }else {
            return boardService.findByKeyword(search, pageable);
        }
    }

    @GetMapping("/write")
    public String addBoardForm(@ModelAttribute("boardDto") BoardDto boardDto, Model model) {
        model.addAttribute("boardDto", boardDto);
        return "/boards/addBoard";
    }

    @PostMapping("/write") //글쓰기 기능
    public String addBoard(@Valid @ModelAttribute("boardDto") BoardDto boardDto) {
        Users user = (Users) httpSession.getAttribute("user");
        boardDto.setUserId(user);
        boardDto.setViews(0L);
        boardDto.setState("1");
        boardDto.setLikesCount(0L);
        boardDto.setCommentsCount(0L);
        boardService.save(boardDto);
        return "redirect:/board";
    }

    @GetMapping("/update/{id}")
    public String updateBoardForm(@PathVariable Long id, Model model) {
        BoardDto boardDto = boardService.getBoardDetail(id);
        model.addAttribute("boardDto", boardDto);
        return "/boards/updateBoard";
    }

    @PostMapping("/update/{id}")
    public String updateBoard(@PathVariable Long id, @ModelAttribute("boardDto") BoardDto boardDto) {
        BoardDto existingBoard = boardService.getBoardDetail(id);
        existingBoard.setTitle(boardDto.getTitle());
        existingBoard.setContents(boardDto.getContents());

        boardService.save(existingBoard);
        return "redirect:/board/" + id;
    }

    @PostMapping("/deleteBoard/{id}") //글 삭제
    public String deleteBoard(@PathVariable Long id) {
        boardService.delete(id);
        return "redirect:/board";
    }

    @PostMapping("/deleteComment/{id}/{commentId}") //댓글 삭제
    public String deleteComment(@PathVariable Long id, @PathVariable Long commentId) {
       commentsService.deleteComment(commentId);
       boardService.updateCommentCounts(id);
        return "redirect:/board/" + id;
    }


    @PostMapping("/comment/{id}")
    public String addComment(@PathVariable Long id, @ModelAttribute("comment") CommentsDto commentsDto) {
        Users user = (Users) httpSession.getAttribute("user");
        commentsDto.setUserId(user.getId());
        commentsDto.setBoardId(id);
        commentsService.createComment(commentsDto);
        boardService.updateCommentCounts(id);
        return "redirect:/board/" + id; // 댓글 목록 페이지로 리다이렉트
    }

    @GetMapping("/{id}")  // 글 상세
    public String boardDetail(@PathVariable Long id, Model model, @PageableDefault(page = 0, size = 50, sort = "id", direction = Sort.Direction.DESC) Pageable pageable){
        Users user = (Users) httpSession.getAttribute("user");
        BoardDto boardDto = boardService.getBoardDetail(id);
        Long likes = likeService.getLike(id);


        getCommentDetails(id, pageable, model); // 코멘트 가져오기

        if (user != null) {
            getUserLike(id, user, model); // 내 좋아요 가져오기
        }

        boardService.updateViews(id); //조회수 증가


        model.addAttribute("id", id);
        model.addAttribute("boardDto", boardDto);
        model.addAttribute("likes", likes);

        return "/boards/boardDetail";
    }

    private void getCommentDetails(Long boardId, Pageable pageable, Model model) {
        Page<CommentsDto> comments = commentsService.getCommentListWithReplies(boardId, pageable);
        Long commentsCount = commentsService.getCommentsCount(boardId);

        comments.forEach(comment -> comment.setAreAllRepliesDeleted(comment.areAllRepliesDeleted()));
        model.addAttribute("comments", comments);
        model.addAttribute("commentsCount", commentsCount);
    }

    private void getUserLike(Long boardId, Users user, Model model) { // 나의 좋아요(좋아요 누른 게시글)
        Boolean myLike = likeService.findMyLike(boardId, user.getId());
        model.addAttribute("user", user);
        model.addAttribute("myLike", myLike);
    }

    @PostMapping("/likes/{id}")
    public String likes(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) { // 좋아요 기능
        Users user = userService.findUserByEmail(userDetails.getUsername());
        likeService.saveLikes(id, user.getId());
        boardService.updateLikes(id);
        return "redirect:/board/" + id;
    }
}
