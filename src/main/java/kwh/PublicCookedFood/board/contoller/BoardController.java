package kwh.PublicCookedFood.board.contoller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.dto.request.BoardUpdateRequest;
import kwh.PublicCookedFood.board.dto.request.BoardWriteRequest;
import kwh.PublicCookedFood.board.facade.BoardFacade;
import kwh.PublicCookedFood.common.Paging;
import kwh.PublicCookedFood.common.dto.request.BoardSearchQuery;
import kwh.PublicCookedFood.common.web.QueryParamCanonicalizer;
import kwh.PublicCookedFood.config.oauth2.LoginAccount;
import kwh.PublicCookedFood.account.domain.Account;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/boards")
@Hidden
public class BoardController {

    private static final String LOGIN_REDIRECT = "redirect:/u/login";
    private static final String BOARD_LIST_VIEW = "/boards/boardList";
    private static final String BOARD_RESULTS_FRAGMENT_VIEW = "boards/boardList :: boardResults";
    private static final String BOARD_ADD_VIEW = "/boards/addBoard";
    private static final String BOARD_UPDATE_VIEW = "/boards/updateBoard";

    private final BoardFacade boardFacade;

    @GetMapping("")
    public String boardList(@LoginAccount Account account,
                            Model model,
                            @PageableDefault(page = 0, size = 15, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
                            @Valid @ModelAttribute("query") BoardSearchQuery query,
                            BindingResult bindingResult,
                            HttpServletRequest request) {
        return renderBoardList(model, pageable, query, bindingResult, request, false,
                account == null ? null : account.getId());
    }

    @GetMapping("/featured")
    public String featuredBoardList(@LoginAccount Account account,
                                    Model model,
                                    @PageableDefault(page = 0, size = 15, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
                                    @Valid @ModelAttribute("query") BoardSearchQuery query,
                                    BindingResult bindingResult,
                                    HttpServletRequest request) {
        return renderBoardList(model, pageable, query, bindingResult, request, true,
                account == null ? null : account.getId());
    }

    @GetMapping("/scraps")
    public String myScrapBoardList(@LoginAccount Account account, Model model) {
        String loginRedirect = redirectIfUnauthenticated(account);
        if (loginRedirect != null) {
            return loginRedirect;
        }
        List<Board> scrappedBoards = boardFacade.loadMyScrappedBoards(account.getId());
        model.addAttribute("scrappedBoards", scrappedBoards);
        return "/boards/boardScraps";
    }

    private String renderBoardList(Model model,
                                   Pageable pageable,
                                   BoardSearchQuery query,
                                   BindingResult bindingResult,
                                   HttpServletRequest request,
                                   boolean featuredPage,
                                   Long viewerAccountId) {
        boolean ajaxRequest = isAjaxRequest(request);
        String boardListPath = featuredPage ? "/boards/featured" : "/boards";
        if (!ajaxRequest) {
            String canonicalRedirect = QueryParamCanonicalizer.buildRedirectIfHasEmptyValues(request, boardListPath);
            if (canonicalRedirect != null) {
                return canonicalRedirect;
            }
        }

        BoardFacade.BoardListViewData viewData = boardFacade.loadBoardList(
                pageable,
                query,
                featuredPage,
                bindingResult.hasErrors(),
                viewerAccountId
        );

        if (viewData.queryErrorMsg() != null) {
            model.addAttribute("queryErrorMsg", viewData.queryErrorMsg());
        }
        Paging.addPagingAttributes(model, viewData.boardList(), viewData.listPageable());
        applyBoardListAttributes(model, viewData);

        if (ajaxRequest) {
            return BOARD_RESULTS_FRAGMENT_VIEW;
        }
        return BOARD_LIST_VIEW;
    }

    private void applyBoardListAttributes(Model model, BoardFacade.BoardListViewData viewData) {
        model.addAttribute("boardList", viewData.boardList());
        model.addAttribute("keyword", null);
        model.addAttribute("search", viewData.search());
        model.addAttribute("activeSection", viewData.activeSection());
        model.addAttribute("orderBy", viewData.orderBy());
        model.addAttribute("boardViewMap", viewData.boardViewMap());
        model.addAttribute("boardThumbnailMap", viewData.boardThumbnailMap());
        model.addAttribute("boardHasImageMap", viewData.boardHasImageMap());
        model.addAttribute("thumbnailDisplayMode", viewData.thumbnailDisplayMode());
        model.addAttribute("sections", viewData.sections());
        model.addAttribute("boardListPath", viewData.boardListPath());
        model.addAttribute("isFeaturedPage", viewData.featuredPage());
        model.addAttribute("featuredLikeThreshold", viewData.featuredLikeThreshold());
        model.addAttribute("boardPageTitle", viewData.boardPageTitle());
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
    }

    @GetMapping("/new")
    public String addBoardForm(@ModelAttribute("boardDto") BoardWriteRequest boardDto, Model model) {
        boardFacade.applyDefaultSection(boardDto);
        loadBoardSections(model);
        model.addAttribute("boardDto", boardDto);
        return BOARD_ADD_VIEW;
    }

    @PostMapping("")
    public String addBoard(@LoginAccount Account account,
                           @Valid @ModelAttribute("boardDto") BoardWriteRequest boardDto,
                           BindingResult bindingResult,
                           Model model) {
        String loginRedirect = redirectIfUnauthenticated(account);
        if (loginRedirect != null) {
            return loginRedirect;
        }
        if (bindingResult.hasErrors()) {
            boardFacade.applyDefaultSection(boardDto);
            loadBoardSections(model);
            return BOARD_ADD_VIEW;
        }

        boardFacade.createBoard(account.getId(), boardDto);
        return "redirect:/boards";
    }

    @GetMapping("/{id:[0-9]+}/edit")
    public String updateBoardForm(@LoginAccount Account account, @PathVariable Long id, Model model) {
        BoardFacade.BoardManageContext manageContext = boardFacade.loadBoardManageContext(account, id);
        if (!manageContext.manageable()) {
            return boardDetailRedirect(id);
        }

        BoardUpdateRequest boardUpdateRequest = boardFacade.toBoardUpdateRequest(manageContext.board());
        loadBoardSections(model);
        model.addAttribute("boardDto", boardUpdateRequest);
        return BOARD_UPDATE_VIEW;
    }

    @PatchMapping("/{id:[0-9]+}")
    public String updateBoard(@LoginAccount Account account,
                              @PathVariable Long id,
                              @Valid @ModelAttribute("boardDto") BoardUpdateRequest boardDto,
                              BindingResult bindingResult,
                              Model model) {
        BoardFacade.BoardManageContext manageContext = boardFacade.loadBoardManageContext(account, id);
        if (!manageContext.manageable()) {
            return boardDetailRedirect(id);
        }
        if (bindingResult.hasErrors()) {
            boardDto.setId(id);
            boardFacade.applyDefaultSection(boardDto);
            loadBoardSections(model);
            model.addAttribute("boardDto", boardDto);
            return BOARD_UPDATE_VIEW;
        }

        boardFacade.updateBoard(account == null ? null : account.getId(), id, boardDto, manageContext.board());
        return boardDetailRedirect(id);
    }

    @PatchMapping("/{id:[0-9]+}/delete")
    public String deleteBoard(@LoginAccount Account account, @PathVariable Long id) {
        BoardFacade.BoardManageContext manageContext = boardFacade.loadBoardManageContext(account, id);
        if (!manageContext.manageable()) {
            return boardDetailRedirect(id);
        }

        boardFacade.deleteBoard(account == null ? null : account.getId(), id);
        return "redirect:/boards";
    }

    private void loadBoardSections(Model model) {
        model.addAttribute("sections", boardFacade.loadActiveSections());
    }

    private String redirectIfUnauthenticated(Account account) {
        return account == null ? LOGIN_REDIRECT : null;
    }

    private String boardDetailRedirect(Long boardId) {
        return "redirect:/boards/" + boardId;
    }
}
