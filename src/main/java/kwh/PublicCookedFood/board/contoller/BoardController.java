package kwh.PublicCookedFood.board.contoller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.jsoup.Jsoup;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardReportReason;
import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.board.domain.SoftDeleteState;
import kwh.PublicCookedFood.board.dto.request.BoardReportCreateRequest;
import kwh.PublicCookedFood.board.dto.request.BoardUpdateRequest;
import kwh.PublicCookedFood.board.dto.request.BoardSaveRequest;
import kwh.PublicCookedFood.board.dto.request.BoardWriteRequest;
import kwh.PublicCookedFood.board.dto.request.CommentCreateRequest;
import kwh.PublicCookedFood.board.dto.response.BoardDetailResponse;
import kwh.PublicCookedFood.board.dto.response.CommentResponse;
import kwh.PublicCookedFood.board.service.BoardReportService;
import kwh.PublicCookedFood.board.service.BoardSectionService;
import kwh.PublicCookedFood.board.service.BoardScrapService;
import kwh.PublicCookedFood.board.service.BoardService;
import kwh.PublicCookedFood.board.service.CommentsService;
import kwh.PublicCookedFood.board.service.LikeService;
import kwh.PublicCookedFood.common.Paging;
import kwh.PublicCookedFood.common.dto.request.BoardSearchQuery;
import kwh.PublicCookedFood.config.oauth2.LoginUser;
import kwh.PublicCookedFood.notification.service.NotificationService;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.service.UserActivityLogService;
import kwh.PublicCookedFood.user.service.UserBlockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private final BoardScrapService boardScrapService;

    private final BoardReportService boardReportService;

    private final UserBlockService userBlockService;

    private final NotificationService notificationService;

    private final UserActivityLogService userActivityLogService;

    @GetMapping("")
    public String boardList(@LoginUser Users user,
                            Model model, @PageableDefault(page = 0, size = 15, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
                            @Valid @ModelAttribute("query") BoardSearchQuery query,
                            BindingResult bindingResult,
                            HttpServletRequest request) {
        return renderBoardList(model, pageable, query, bindingResult, request, false,
                user == null ? null : user.getId());
    }

    @GetMapping("/featured")
    public String featuredBoardList(@LoginUser Users user,
                                    Model model, @PageableDefault(page = 0, size = 15, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
                                    @Valid @ModelAttribute("query") BoardSearchQuery query,
                                    BindingResult bindingResult,
                                    HttpServletRequest request) {
        return renderBoardList(model, pageable, query, bindingResult, request, true,
                user == null ? null : user.getId());
    }

    @GetMapping("/scraps")
    public String myScrapBoardList(@LoginUser Users user, Model model) {
        if (user == null) {
            return "redirect:/u/login";
        }
        Set<Long> blockedUserIds = userBlockService.getViewRestrictedUserIds(user.getId());
        List<Board> scrappedBoards = boardScrapService.getMyScrappedBoards(user.getId(), blockedUserIds);
        model.addAttribute("scrappedBoards", scrappedBoards);
        return "/boards/boardScraps";
    }

    private String renderBoardList(Model model,
                                   Pageable pageable,
                                   BoardSearchQuery query,
                                   BindingResult bindingResult,
                                   HttpServletRequest request,
                                   boolean featuredPage,
                                   Long viewerUserId) {
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
        String orderBy = normalizeOrderBy(query.normalizedOrderBy(), featuredPage);
        if (bindingResult.hasErrors()) {
            search = null;
            section = null;
            orderBy = null;
            model.addAttribute("queryErrorMsg", "검색 조건이 유효하지 않아 기본 목록을 표시합니다.");
        }

        if (section != null && !boardSectionService.existsActiveSection(section)) {
            section = null;
            if (!model.containsAttribute("queryErrorMsg")) {
                model.addAttribute("queryErrorMsg", "존재하지 않거나 비활성화된 게시판 탭입니다.");
            }
        }

        Pageable listPageable = resolveListPageable(pageable, featuredPage, orderBy);
        Set<Long> blockedUserIds = userBlockService.getViewRestrictedUserIds(viewerUserId);
        Page<Board> boardList = getBoardInfo(listPageable, search, section, featuredPage, blockedUserIds);
        Paging.addPagingAttributes(model, boardList, listPageable); //페이징 알고리즘

        BoardThumbnailData thumbnailData = extractBoardThumbnailData(boardList.getContent());
        String thumbnailDisplayMode = boardService.getThumbnailDisplayMode().name();

        model.addAttribute("boardList", boardList);
        model.addAttribute("keyword", null);
        model.addAttribute("search", search);
        model.addAttribute("activeSection", section);
        model.addAttribute("orderBy", orderBy);
        model.addAttribute("boardThumbnailMap", thumbnailData.thumbnailUrlByBoardId());
        model.addAttribute("boardHasImageMap", thumbnailData.hasImageByBoardId());
        model.addAttribute("thumbnailDisplayMode", thumbnailDisplayMode);
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

    private Page<Board> getBoardInfo(Pageable pageable,
                                     String search,
                                     String section,
                                     boolean featuredPage,
                                     Set<Long> blockedUserIds) {
        if (featuredPage) {
            if (search == null || search.isEmpty()) {
                return boardService.getFeaturedBoardList(pageable, section, null, blockedUserIds);
            } else {
                return boardService.findFeaturedByKeyword(search, section, null, blockedUserIds, pageable);
            }
        }

        if (search == null || search.isEmpty()) {
            return boardService.getBoardList(pageable, section, null, blockedUserIds);
        } else {
            return boardService.findByKeyword(search, section, null, blockedUserIds, pageable);
        }
    }

    private Pageable resolveListPageable(Pageable pageable, boolean featuredPage, String orderBy) {
        if (featuredPage) {
            return pageable;
        }
        String normalizedOrderBy = (orderBy == null || orderBy.isBlank()) ? "recent" : orderBy;
        Sort sort = switch (normalizedOrderBy) {
            case "views" -> Sort.by(Sort.Order.desc("views"));
            case "likes" -> Sort.by(Sort.Order.desc("likeCount"));
            case "comments" -> Sort.by(Sort.Order.desc("commentCount"));
            default -> Sort.by(Sort.Order.desc("regTime"));
        };
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    private String normalizeOrderBy(String orderBy, boolean featuredPage) {
        if (featuredPage || orderBy == null) {
            return null;
        }
        return switch (orderBy) {
            case "views", "likes", "comments" -> orderBy;
            default -> null;
        };
    }

    private BoardThumbnailData extractBoardThumbnailData(List<Board> boards) {
        if (boards == null || boards.isEmpty()) {
            return new BoardThumbnailData(Map.of(), Map.of());
        }

        Map<Long, String> thumbnailUrlByBoardId = new HashMap<>();
        Map<Long, Boolean> hasImageByBoardId = new HashMap<>();

        for (Board board : boards) {
            Long boardId = board.getId();
            if (boardId == null) {
                continue;
            }
            String thumbnailUrl = extractFirstImageUrl(board.getContents());
            if (thumbnailUrl != null) {
                thumbnailUrlByBoardId.put(boardId, thumbnailUrl);
                hasImageByBoardId.put(boardId, true);
            } else {
                hasImageByBoardId.put(boardId, false);
            }
        }

        return new BoardThumbnailData(thumbnailUrlByBoardId, hasImageByBoardId);
    }

    private String extractFirstImageUrl(String htmlContent) {
        if (htmlContent == null || htmlContent.isBlank()) {
            return null;
        }

        String src = Jsoup.parse(htmlContent)
                .select("img[src]")
                .stream()
                .map(element -> element.attr("src"))
                .map(value -> value == null ? "" : value.trim())
                .filter(value -> !value.isBlank())
                .filter(value -> !value.startsWith("data:"))
                .findFirst()
                .orElse(null);

        return src == null || src.isBlank() ? null : src;
    }

    private record BoardThumbnailData(Map<Long, String> thumbnailUrlByBoardId,
                                      Map<Long, Boolean> hasImageByBoardId) {
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
        Board savedBoard = boardService.save(command);
        notificationService.notifyOnBoardCreated(savedBoard);
        userActivityLogService.record(
                user.getId(),
                "BOARD_CREATE",
                "boardId=" + (savedBoard.getId() == null ? "-" : savedBoard.getId())
        );
        return "redirect:/boards";
    }

    @GetMapping("/{id:[0-9]+}/edit")
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
    @PatchMapping("/{id:[0-9]+}")
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
        userActivityLogService.record(
                user.getId(),
                "BOARD_UPDATE",
                "boardId=" + id
        );
        return "redirect:/boards/" + id;
    }

    @PatchMapping("/{id:[0-9]+}/delete") //글 삭제(update)
    public String deleteBoard(@LoginUser Users user, @PathVariable Long id) {
        BoardDetailResponse boardDto = boardService.getBoardDetail(id);
        if (user == null || boardDto.getUserId() == null || !boardDto.getUserId().equals(user.getId())) {
            return "redirect:/boards/" + id;
        }

        boardService.delete(id);
        log.info("action=board.delete result=success userId={} boardId={}", user.getId(), id);
        userActivityLogService.record(user.getId(), "BOARD_DELETE", "boardId=" + id);
        return "redirect:/boards";
    }

    @PatchMapping("/{id:[0-9]+}/comments/{commentId:[0-9]+}/delete") //댓글 삭제(update)
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
       log.info("action=board.comment_delete result=success userId={} boardId={} commentId={}",
               user.getId(), id, commentId);
       userActivityLogService.record(
               user.getId(),
               "BOARD_COMMENT_DELETE",
               "boardId=" + id + ",commentId=" + commentId
       );
        return "redirect:/boards/" + id;
    }

    //댓글 등록
    @PostMapping("/{id:[0-9]+}/comments")
    public String addComment(@LoginUser Users user,
                             @PathVariable Long id,
                             @Valid @ModelAttribute("comment") CommentCreateRequest commentsDto,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes) {
        if (user == null) {
            return "redirect:/u/login";
        }
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("commentError", "댓글 내용을 확인해주세요.");
            return "redirect:/boards/" + id;
        }

        try {
            commentsDto.setUserId(user.getId());
            commentsDto.setBoardId(id);
            CommentResponse savedComment = commentsService.createComment(commentsDto);
            boardService.updateCommentCounts(id);
            log.info("action=board.comment_create result=success userId={} boardId={} parentId={}",
                    user.getId(), id, commentsDto.getParentId());
            userActivityLogService.record(
                    user.getId(),
                    "BOARD_COMMENT_CREATE",
                    "boardId=" + id
                            + ",commentId=" + (savedComment.getId() == null ? "-" : savedComment.getId())
                            + ",parentId=" + (commentsDto.getParentId() == null ? "-" : commentsDto.getParentId())
            );
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("action=board.comment_create result=failed userId={} boardId={} reason={}",
                    user.getId(), id, e.getMessage());
            redirectAttributes.addFlashAttribute("commentError", e.getMessage());
        }
        return "redirect:/boards/" + id; // 댓글 목록 페이지로 리다이렉트
    }

    @PostMapping("/{id:[0-9]+}/scraps")
    public String addScrap(@PathVariable Long id, @LoginUser Users user) {
        if (user == null) {
            return "redirect:/u/login";
        }
        boardScrapService.addScrap(id, user.getId());
        userActivityLogService.record(user.getId(), "BOARD_SCRAP_ADD", "boardId=" + id);
        return "redirect:/boards/" + id;
    }

    @PatchMapping("/{id:[0-9]+}/scraps/delete")
    public String deleteScrap(@PathVariable Long id, @LoginUser Users user) {
        if (user == null) {
            return "redirect:/u/login";
        }
        boardScrapService.removeScrap(id, user.getId());
        userActivityLogService.record(user.getId(), "BOARD_SCRAP_REMOVE", "boardId=" + id);
        return "redirect:/boards/" + id;
    }

    @PostMapping("/{id:[0-9]+}/reports")
    public String reportBoard(@PathVariable Long id,
                              @LoginUser Users user,
                              @Valid @ModelAttribute("reportDto") BoardReportCreateRequest reportDto,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes) {
        if (user == null) {
            return "redirect:/u/login";
        }
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("reportError", "신고 사유를 확인해주세요.");
            return "redirect:/boards/" + id;
        }

        try {
            boardReportService.createReport(id, user.getId(), reportDto.getReason(), reportDto.getDetails());
            redirectAttributes.addFlashAttribute("reportSuccess", "신고가 접수되었습니다.");
            log.info("action=board.report result=success userId={} boardId={} reportReason={}",
                    user.getId(), id, reportDto.getReason());
            userActivityLogService.record(
                    user.getId(),
                    "BOARD_REPORT_CREATE",
                    "boardId=" + id + ",reason=" + reportDto.getReason().name()
            );
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("action=board.report result=failed userId={} boardId={} reason={}",
                    user.getId(), id, e.getMessage());
            redirectAttributes.addFlashAttribute("reportError", e.getMessage());
        }
        return "redirect:/boards/" + id;
    }

    @GetMapping("/{id:[0-9]+}")  // 글 상세
    public String boardDetail(@LoginUser Users user, @PathVariable Long id, Model model, @PageableDefault(page = 0, size = 50, sort = "regTime", direction = Sort.Direction.ASC) Pageable pageable){
        BoardDetailResponse boardDto = boardService.getBoardDetail(id);
        if (user != null
                && boardDto.getUserId() != null
                && userBlockService.isEitherBlocked(user.getId(), boardDto.getUserId())) {
            throw new IllegalStateException("차단 관계인 사용자의 게시글은 조회할 수 없습니다.");
        }
        boardService.updateViews(id); //조회수 증가
        boardDto = boardService.getBoardDetail(id);
        Long likes = likeService.getLike(id);
        long scraps = boardScrapService.getScrapCount(id);

        Long currentUserId = null;
        boolean myScrap = false;
        boolean myReport = false;
        boolean myBlockedAuthor = false;
        boolean boardInteractionBlocked = false;
        if (user != null) {
            currentUserId = user.getId();
            getUserLike(id, user, model); // 내 좋아요 가져오기
            myScrap = boardScrapService.isScrapped(id, user.getId());
            myReport = boardReportService.hasReported(id, user.getId());
            if (boardDto.getUserId() != null && !boardDto.getUserId().equals(user.getId())) {
                myBlockedAuthor = userBlockService.isBlocked(user.getId(), boardDto.getUserId());
            }
        }

        getCommentDetails(id, pageable, model, currentUserId); // 코멘트 가져오기

        model.addAttribute("id", id);
        model.addAttribute("boardDto", boardDto);
        model.addAttribute("likes", likes);
        model.addAttribute("scraps", scraps);
        model.addAttribute("myScrap", myScrap);
        model.addAttribute("myReport", myReport);
        model.addAttribute("myBlockedAuthor", myBlockedAuthor);
        model.addAttribute("boardInteractionBlocked", boardInteractionBlocked);
        model.addAttribute("reportReasons", BoardReportReason.values());
        model.addAttribute("currentUserId", currentUserId);

        return "/boards/boardDetail";
    }

    private void getCommentDetails(Long boardId, Pageable pageable, Model model, Long currentUserId) { // 댓글 목록 가져오기
        Page<CommentResponse> comments = commentsService.getCommentListWithReplies(boardId, pageable, currentUserId);
        Long commentsCount = commentsService.getCommentsCount(boardId, currentUserId);

        comments.forEach(comment -> comment.setAreAllRepliesDeleted(comment.areAllRepliesDeleted()));
        model.addAttribute("comments", comments);
        model.addAttribute("commentsCount", commentsCount);
    }

    private void getUserLike(Long boardId, Users user, Model model) { // 나의 좋아요(좋아요 누른 게시글)
        Boolean myLike = likeService.findMyLike(boardId, user.getId());
        model.addAttribute("myLike", myLike);
    }

    @PutMapping("/{id:[0-9]+}/likes")
    public String likes(@PathVariable Long id, @LoginUser Users user) { // 좋아요 기능
        if (user == null) {
            return "redirect:/u/login";
        }

        likeService.saveLikes(id, user.getId());
        boardService.updateLikes(id);
        userActivityLogService.record(user.getId(), "BOARD_LIKE_TOGGLE", "boardId=" + id);
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
