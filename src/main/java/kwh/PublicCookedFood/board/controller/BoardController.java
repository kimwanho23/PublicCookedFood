package kwh.PublicCookedFood.board.controller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kwh.PublicCookedFood.board.application.query.view.BoardListPageView;
import kwh.PublicCookedFood.board.application.query.view.BoardScrapListView;
import kwh.PublicCookedFood.board.controller.support.BoardWriteFormSupport;
import kwh.PublicCookedFood.board.controller.support.BoardViewerSupport;
import kwh.PublicCookedFood.board.dto.request.BoardSearchQuery;
import kwh.PublicCookedFood.board.dto.request.BoardUpdateRequest;
import kwh.PublicCookedFood.board.dto.request.BoardWriteRequest;
import kwh.PublicCookedFood.board.facade.BoardListQueryFacade;
import kwh.PublicCookedFood.board.facade.BoardScrapQueryFacade;
import kwh.PublicCookedFood.board.facade.BoardViewer;
import kwh.PublicCookedFood.board.service.command.BoardCommandService;
import kwh.PublicCookedFood.board.service.command.BoardCreateCommand;
import kwh.PublicCookedFood.board.service.command.BoardSaveCommand;
import kwh.PublicCookedFood.board.service.command.BoardUpdateCommand;
import kwh.PublicCookedFood.common.Paging;
import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.common.error.CommonErrorCode;
import kwh.PublicCookedFood.common.web.QueryParamCanonicalizer;
import kwh.PublicCookedFood.config.oauth2.LoginAccount;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.storage.StorageException;
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

@Controller
@RequiredArgsConstructor
@RequestMapping("/boards")
@Hidden
public class BoardController {

    private static final String BOARD_LIST_VIEW = "boards/boardList";
    private static final String BOARD_RESULTS_FRAGMENT_VIEW = "boards/boardList :: boardResults";
    private static final String BOARD_ADD_VIEW = "boards/addBoard";
    private static final String BOARD_UPDATE_VIEW = "boards/updateBoard";

    private final BoardListQueryFacade boardListQueryFacade;
    private final BoardScrapQueryFacade boardScrapQueryFacade;
    private final BoardWriteFormSupport boardWriteFormSupport;
    private final BoardCommandService boardCommandService;
    private final BoardViewerSupport boardViewerSupport;

    @GetMapping("")
    public String boardList(@LoginAccount Account account,
                            Model model,
                            @PageableDefault(page = 0, size = 15, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
                            @Valid @ModelAttribute("query") BoardSearchQuery query,
                            BindingResult bindingResult,
                            HttpServletRequest request) {
        BoardViewer viewer = BoardViewer.from(account);
        return renderBoardList(model, pageable, query, bindingResult, request, false, viewer);
    }

    @GetMapping("/featured")
    public String featuredBoardList(@LoginAccount Account account,
                                    Model model,
                                    @PageableDefault(page = 0, size = 15, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
                                    @Valid @ModelAttribute("query") BoardSearchQuery query,
                                    BindingResult bindingResult,
                                    HttpServletRequest request) {
        BoardViewer viewer = BoardViewer.from(account);
        return renderBoardList(model, pageable, query, bindingResult, request, true, viewer);
    }

    @GetMapping("/scraps")
    public String myScrapBoardList(@LoginAccount Account account, Model model) {
        BoardViewer viewer = BoardViewer.from(account);
        if (boardViewerSupport.requiresLogin(viewer)) {
            return boardViewerSupport.loginRedirect();
        }
        BoardScrapListView viewData =
                boardScrapQueryFacade.loadMyScrappedBoards(viewer.requireAuthenticated().accountId());
        model.addAttribute("scrappedBoards", viewData.boards());
        return "boards/boardScraps";
    }

    private String renderBoardList(Model model,
                                   Pageable pageable,
                                   BoardSearchQuery query,
                                   BindingResult bindingResult,
                                   HttpServletRequest request,
                                   boolean featuredPage,
                                   BoardViewer viewer) {
        boolean ajaxRequest = isAjaxRequest(request);
        String boardListPath = featuredPage ? "/boards/featured" : "/boards";
        if (!ajaxRequest) {
            String canonicalRedirect = QueryParamCanonicalizer.buildRedirectIfHasEmptyValues(request, boardListPath);
            if (canonicalRedirect != null) {
                return canonicalRedirect;
            }
        }

        BoardListPageView viewData = boardListQueryFacade.loadBoardList(
                pageable,
                query,
                featuredPage,
                bindingResult.hasErrors(),
                viewer
        );

        viewData.maybeQueryErrorMsg().ifPresent(message -> model.addAttribute("queryErrorMsg", message));
        Paging.addPagingAttributes(model, viewData.boardList(), viewData.listPageable());
        applyBoardListAttributes(model, viewData);

        if (ajaxRequest) {
            return BOARD_RESULTS_FRAGMENT_VIEW;
        }
        return BOARD_LIST_VIEW;
    }

    private void applyBoardListAttributes(Model model, BoardListPageView viewData) {
        model.addAttribute("boardList", viewData.boardList());
        model.addAttribute("keyword", null);
        model.addAttribute("search", viewData.search());
        model.addAttribute("activeSection", viewData.activeSection());
        model.addAttribute("orderBy", viewData.orderBy());
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
        return renderBoardWriteForm(model, boardDto, null);
    }

    @PostMapping("")
    public String addBoard(@LoginAccount Account account,
                           @Valid @ModelAttribute("boardDto") BoardWriteRequest boardDto,
                           BindingResult bindingResult,
                           Model model) {
        BoardViewer viewer = BoardViewer.from(account);
        if (boardViewerSupport.requiresLogin(viewer)) {
            return boardViewerSupport.loginRedirect();
        }
        if (bindingResult.hasErrors()) {
            return renderBoardWriteForm(model, boardDto, null);
        }

        try {
            BoardWriteRequest preparedBoard = boardWriteFormSupport.prepareBoardWriteRequest(boardDto);
            boardCommandService.create(
                    BoardCreateCommand.of(
                            viewer.requireAuthenticated().accountId(),
                            BoardSaveCommand.forCreate(
                                    preparedBoard.getTitle(),
                                    preparedBoard.getContents(),
                                    preparedBoard.getSectionId()
                            )
                    )
            );
            return "redirect:/boards";
        } catch (StorageException e) {
            return renderBoardWriteForm(model, boardDto, e.getMessage());
        }
    }

    @GetMapping("/{id:[0-9]+}/edit")
    public String updateBoardForm(@LoginAccount Account account, @PathVariable Long id, Model model) {
        BoardWriteFormSupport.BoardManageContext manageContext = boardWriteFormSupport.loadBoardManageContext(account, id);
        if (!manageContext.manageable()) {
            return boardDetailRedirect(id);
        }

        BoardUpdateRequest boardUpdateRequest = boardWriteFormSupport.toBoardUpdateRequest(manageContext.board());
        return renderPreparedBoardUpdateForm(model, boardUpdateRequest, null);
    }

    @PatchMapping("/{id:[0-9]+}")
    public String updateBoard(@LoginAccount Account account,
                              @PathVariable Long id,
                              @Valid @ModelAttribute("boardDto") BoardUpdateRequest boardDto,
                              BindingResult bindingResult,
                              Model model) {
        BoardViewer viewer = BoardViewer.from(account);
        BoardWriteFormSupport.BoardManageContext manageContext = boardWriteFormSupport.loadBoardManageContext(account, id);
        if (!manageContext.manageable()) {
            return boardDetailRedirect(id);
        }
        if (bindingResult.hasErrors()) {
            return renderBoardUpdateForm(model, id, boardDto, null);
        }

        try {
            BoardUpdateRequest preparedBoard = boardWriteFormSupport.prepareBoardUpdateRequest(id, boardDto);
            boardCommandService.update(
                    BoardUpdateCommand.of(
                            viewer.requireAuthenticated().accountId(),
                            id,
                            preparedBoard.getVersion(),
                            preparedBoard.getTitle(),
                            preparedBoard.getContents(),
                            preparedBoard.getSectionId()
                    )
            );
            return boardDetailRedirect(id);
        } catch (AppException e) {
            if (e.getErrorCode() != CommonErrorCode.REQUEST_CONFLICT) {
                throw e;
            }
            return renderBoardUpdateForm(model, id, boardDto, e.getMessage());
        } catch (StorageException e) {
            return renderBoardUpdateForm(model, id, boardDto, e.getMessage());
        }
    }

    @PatchMapping("/{id:[0-9]+}/delete")
    public String deleteBoard(@LoginAccount Account account, @PathVariable Long id) {
        BoardWriteFormSupport.BoardManageContext manageContext = boardWriteFormSupport.loadBoardManageContext(account, id);
        if (!manageContext.manageable()) {
            return boardDetailRedirect(id);
        }

        BoardViewer viewer = BoardViewer.from(account);
        boardCommandService.delete(id, viewer.requireAuthenticated().accountId());
        return "redirect:/boards";
    }

    private void loadBoardSections(Model model) {
        model.addAttribute("sections", boardWriteFormSupport.loadActiveSections());
    }

    private String renderBoardWriteForm(Model model, BoardWriteRequest boardDto, String errorMessage) {
        BoardWriteRequest preparedBoard = boardWriteFormSupport.prepareBoardWriteRequest(boardDto);
        return renderPreparedBoardWriteForm(model, preparedBoard, errorMessage);
    }

    private String renderPreparedBoardWriteForm(Model model, BoardWriteRequest preparedBoard, String errorMessage) {
        loadBoardSections(model);
        model.addAttribute("boardDto", preparedBoard);
        applyErrorMessage(model, errorMessage);
        return BOARD_ADD_VIEW;
    }

    private String renderBoardUpdateForm(Model model, Long boardId, BoardUpdateRequest boardDto, String errorMessage) {
        BoardUpdateRequest preparedBoard = boardWriteFormSupport.prepareBoardUpdateRequest(boardId, boardDto);
        return renderPreparedBoardUpdateForm(model, preparedBoard, errorMessage);
    }

    private String renderPreparedBoardUpdateForm(Model model, Object preparedBoard, String errorMessage) {
        loadBoardSections(model);
        model.addAttribute("boardDto", preparedBoard);
        applyErrorMessage(model, errorMessage);
        return BOARD_UPDATE_VIEW;
    }

    private void applyErrorMessage(Model model, String errorMessage) {
        if (errorMessage != null && !errorMessage.isBlank()) {
            model.addAttribute("errorMessage", errorMessage);
        }
    }

    private String boardDetailRedirect(Long boardId) {
        return "redirect:/boards/" + boardId;
    }
}
