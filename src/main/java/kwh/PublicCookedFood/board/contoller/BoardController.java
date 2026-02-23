package kwh.PublicCookedFood.board.contoller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.board.domain.SoftDeleteState;
import kwh.PublicCookedFood.board.dto.request.BoardUpdateRequest;
import kwh.PublicCookedFood.board.dto.request.BoardSaveRequest;
import kwh.PublicCookedFood.board.dto.request.BoardWriteRequest;
import kwh.PublicCookedFood.board.dto.request.CommentCreateRequest;
import kwh.PublicCookedFood.board.dto.response.BoardDetailResponse;
import kwh.PublicCookedFood.board.dto.response.CommentResponse;
import kwh.PublicCookedFood.board.service.BoardSectionService;
import kwh.PublicCookedFood.board.service.BoardService;
import kwh.PublicCookedFood.board.service.CommentsService;
import kwh.PublicCookedFood.board.service.LikeService;
import kwh.PublicCookedFood.common.Paging;
import kwh.PublicCookedFood.common.dto.request.BoardSearchQuery;
import kwh.PublicCookedFood.config.oauth2.LoginUser;
import kwh.PublicCookedFood.user.domain.Users;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/boards")
@Hidden
public class BoardController {

    private final BoardService boardService;

    private final CommentsService commentsService;

    private final LikeService likeService;

    private final BoardSectionService boardSectionService;

    @GetMapping("")
    public String boardList(Model model, @PageableDefault(page = 0, size = 15, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
                            @Valid @ModelAttribute("query") BoardSearchQuery query,
                            BindingResult bindingResult,
                            HttpServletRequest request) {
        return renderBoardList(model, pageable, query, bindingResult, request, false);
    }

    @GetMapping("/featured")
    public String featuredBoardList(Model model, @PageableDefault(page = 0, size = 15, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
                                    @Valid @ModelAttribute("query") BoardSearchQuery query,
                                    BindingResult bindingResult,
                                    HttpServletRequest request) {
        return renderBoardList(model, pageable, query, bindingResult, request, true);
    }

    private String renderBoardList(Model model,
                                   Pageable pageable,
                                   BoardSearchQuery query,
                                   BindingResult bindingResult,
                                   HttpServletRequest request,
                                   boolean featuredPage) {
        boolean ajaxRequest = isAjaxRequest(request);
        String boardListPath = featuredPage ? "/boards/featured" : "/boards";
        if (!ajaxRequest) {
            String canonicalRedirect = buildCanonicalBoardListRedirect(request, boardListPath);
            if (canonicalRedirect != null) {
                return canonicalRedirect;
            }
        }

        String search = query.normalizedSearch();
        String section = query.normalizedSection();
        if (bindingResult.hasErrors()) {
            search = null;
            section = null;
            model.addAttribute("queryErrorMsg", "검색 조건이 유효하지 않아 기본 목록을 표시합니다.");
        }

        if (section != null && !boardSectionService.existsActiveSection(section)) {
            section = null;
            if (!model.containsAttribute("queryErrorMsg")) {
                model.addAttribute("queryErrorMsg", "존재하지 않거나 비활성화된 게시판 탭입니다.");
            }
        }

        Page<Board> boardList = getBoardInfo(pageable, search, section, featuredPage);
        Paging.addPagingAttributes(model, boardList, pageable); //페이징 알고리즘

        model.addAttribute("boardList", boardList);
        model.addAttribute("keyword", null);
        model.addAttribute("search", search);
        model.addAttribute("activeSection", section);
        model.addAttribute("sections", boardSectionService.getActiveSections());
        model.addAttribute("boardListPath", boardListPath);
        model.addAttribute("isFeaturedPage", featuredPage);
        model.addAttribute("featuredLikeThreshold", boardService.getFeaturedLikeThreshold());
        model.addAttribute("boardPageTitle", featuredPage ? "추천 게시물" : "게시판");

        if (ajaxRequest) {
            return "boards/boardList :: boardResults";
        }
        return "/boards/boardList";
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
    }

    private String buildCanonicalBoardListRedirect(HttpServletRequest request, String basePath) {
        boolean hasEmptyParam = request.getParameterMap().values().stream()
                .flatMap(Arrays::stream)
                .anyMatch(value -> value == null || value.trim().isEmpty());
        if (!hasEmptyParam) {
            return null;
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(basePath);
        request.getParameterMap().forEach((key, values) -> {
            if (values == null) {
                return;
            }
            for (String value : values) {
                if (value != null && !value.trim().isEmpty()) {
                    builder.queryParam(key, value.trim());
                }
            }
        });
        return "redirect:" + builder.build().encode().toUriString();
    }

    private Page<Board> getBoardInfo(Pageable pageable, String search, String section, boolean featuredPage) {
        if (featuredPage) {
            if (search == null || search.isEmpty()) {
                return boardService.getFeaturedBoardList(pageable, section);
            } else {
                return boardService.findFeaturedByKeyword(search, section, pageable);
            }
        }

        if (search == null || search.isEmpty()) {
            return boardService.getBoardList(pageable, section);
        } else {
            return boardService.findByKeyword(search, section, pageable);
        }
    }

    @GetMapping("/new")
    public String addBoardForm(@ModelAttribute("boardDto") BoardWriteRequest boardDto, Model model) {
        applyDefaultSection(boardDto);
        loadBoardSections(model);
        model.addAttribute("boardDto", boardDto);
        return "/boards/addBoard";
    }

    @PostMapping("") //글쓰기 기능
    public String addBoard(@LoginUser Users user,
                           @Valid @ModelAttribute("boardDto") BoardWriteRequest boardDto,
                           BindingResult bindingResult,
                           Model model) {
        if (user == null) {
            return "redirect:/u/login";
        }
        if (bindingResult.hasErrors()) {
            applyDefaultSection(boardDto);
            loadBoardSections(model);
            return "/boards/addBoard";
        }

        BoardSaveRequest command = BoardSaveRequest.builder()
                .title(boardDto.getTitle())
                .contents(boardDto.getContents())
                .userId(user.getId())
                .sectionId(boardDto.getSectionId())
                .views(0L)
                .state(SoftDeleteState.ACTIVE)
                .likesCount(0L)
                .commentsCount(0L)
                .build();
        boardService.save(command);
        return "redirect:/boards";
    }

    @GetMapping("/{id}/edit")
    public String updateBoardForm(@LoginUser Users user, @PathVariable Long id, Model model) {
        BoardDetailResponse boardDto = boardService.getBoardDetail(id);
        if (user == null || boardDto.getUserId() == null || !boardDto.getUserId().equals(user.getId())) {
            return "redirect:/boards/" + id;
        }
        BoardUpdateRequest boardUpdateRequest = BoardUpdateRequest.builder()
                .id(boardDto.getId())
                .title(boardDto.getTitle())
                .contents(boardDto.getContents())
                .sectionId(boardDto.getSectionId() != null ? boardDto.getSectionId() : boardSectionService.ensureDefaultSection().getId())
                .build();
        loadBoardSections(model);
        model.addAttribute("boardDto", boardUpdateRequest);
        return "/boards/updateBoard";
    }

    // 글 수정
    @PatchMapping("/{id}")
    public String updateBoard(@LoginUser Users user,
                              @PathVariable Long id,
                              @Valid @ModelAttribute("boardDto") BoardUpdateRequest boardDto,
                              BindingResult bindingResult,
                              Model model) {
        BoardDetailResponse existingBoard = boardService.getBoardDetail(id);
        if (user == null || existingBoard.getUserId() == null || !existingBoard.getUserId().equals(user.getId())) {
            return "redirect:/boards/" + id;
        }
        if (bindingResult.hasErrors()) {
            boardDto.setId(id);
            applyDefaultSection(boardDto);
            loadBoardSections(model);
            model.addAttribute("boardDto", boardDto);
            return "/boards/updateBoard";
        }

        BoardSaveRequest command = BoardSaveRequest.builder()
                .id(existingBoard.getId())
                .title(boardDto.getTitle())
                .contents(boardDto.getContents())
                .userId(existingBoard.getUserId())
                .sectionId(boardDto.getSectionId())
                .views(existingBoard.getViews())
                .likesCount(existingBoard.getLikesCount())
                .commentsCount(existingBoard.getCommentsCount())
                .state(existingBoard.getState())
                .build();
        boardService.save(command);
        return "redirect:/boards/" + id;
    }

    @PatchMapping("/{id}/delete") //글 삭제(update)
    public String deleteBoard(@LoginUser Users user, @PathVariable Long id) {
        BoardDetailResponse boardDto = boardService.getBoardDetail(id);
        if (user == null || boardDto.getUserId() == null || !boardDto.getUserId().equals(user.getId())) {
            return "redirect:/boards/" + id;
        }

        boardService.delete(id);
        return "redirect:/boards";
    }

    @PatchMapping("/{id}/comments/{commentId}/delete") //댓글 삭제(update)
    public String deleteComment(@LoginUser Users user, @PathVariable Long id, @PathVariable Long commentId) {
       if (user == null) {
           return "redirect:/u/login";
       }

       Comments comment = commentsService.getComment(commentId);
       if (!comment.getBoard().getId().equals(id) || !comment.getUser().getId().equals(user.getId())) {
           return "redirect:/boards/" + id;
       }

       commentsService.deleteComment(commentId);
       boardService.updateCommentCounts(id);
        return "redirect:/boards/" + id;
    }

    //댓글 등록
    @PostMapping("/{id}/comments")
    public String addComment(@LoginUser Users user,
                             @PathVariable Long id,
                             @Valid @ModelAttribute("comment") CommentCreateRequest commentsDto,
                             BindingResult bindingResult) {
        if (user == null) {
            return "redirect:/u/login";
        }
        if (bindingResult.hasErrors()) {
            return "redirect:/boards/" + id;
        }

        commentsDto.setUserId(user.getId());
        commentsDto.setBoardId(id);
        commentsService.createComment(commentsDto);
        boardService.updateCommentCounts(id);
        return "redirect:/boards/" + id; // 댓글 목록 페이지로 리다이렉트
    }

    @GetMapping("/{id}")  // 글 상세
    public String boardDetail(@LoginUser Users user, @PathVariable Long id, Model model, @PageableDefault(page = 0, size = 50, sort = "regTime", direction = Sort.Direction.ASC) Pageable pageable){
        boardService.updateViews(id); //조회수 증가
        BoardDetailResponse boardDto = boardService.getBoardDetail(id);
        Long likes = likeService.getLike(id);

        getCommentDetails(id, pageable, model); // 코멘트 가져오기

        Long currentUserId = null;
        if (user != null) {
            currentUserId = user.getId();
            getUserLike(id, user, model); // 내 좋아요 가져오기
        }

        model.addAttribute("id", id);
        model.addAttribute("boardDto", boardDto);
        model.addAttribute("likes", likes);
        model.addAttribute("currentUserId", currentUserId);

        return "/boards/boardDetail";
    }

    private void getCommentDetails(Long boardId, Pageable pageable, Model model) { // 댓글 목록 가져오기
        Page<CommentResponse> comments = commentsService.getCommentListWithReplies(boardId, pageable);
        Long commentsCount = commentsService.getCommentsCount(boardId);

        comments.forEach(comment -> comment.setAreAllRepliesDeleted(comment.areAllRepliesDeleted()));
        model.addAttribute("comments", comments);
        model.addAttribute("commentsCount", commentsCount);
    }

    private void getUserLike(Long boardId, Users user, Model model) { // 나의 좋아요(좋아요 누른 게시글)
        Boolean myLike = likeService.findMyLike(boardId, user.getId());
        model.addAttribute("myLike", myLike);
    }

    @PutMapping("/{id}/likes")
    public String likes(@PathVariable Long id, @LoginUser Users user) { // 좋아요 기능
        if (user == null) {
            return "redirect:/u/login";
        }

        likeService.saveLikes(id, user.getId());
        boardService.updateLikes(id);
        return "redirect:/boards/" + id;
    }

    private void applyDefaultSection(BoardWriteRequest boardDto) {
        if (boardDto.getSectionId() == null) {
            boardDto.setSectionId(boardSectionService.ensureDefaultSection().getId());
        }
    }

    private void applyDefaultSection(BoardUpdateRequest boardDto) {
        if (boardDto.getSectionId() == null) {
            boardDto.setSectionId(boardSectionService.ensureDefaultSection().getId());
        }
    }

    private void loadBoardSections(Model model) {
        model.addAttribute("sections", boardSectionService.getActiveSections());
    }
}
