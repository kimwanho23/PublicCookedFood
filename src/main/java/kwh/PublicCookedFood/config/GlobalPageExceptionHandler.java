package kwh.PublicCookedFood.config;

import jakarta.servlet.http.HttpServletRequest;
import kwh.PublicCookedFood.board.contoller.AdminBoardPageController;
import kwh.PublicCookedFood.board.contoller.BoardController;
import kwh.PublicCookedFood.board.contoller.BoardDetailController;
import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.common.error.CommonErrorCode;
import kwh.PublicCookedFood.common.error.ErrorMessageResolver;
import kwh.PublicCookedFood.food.controller.MainController;
import kwh.PublicCookedFood.food.controller.RecipeController;
import kwh.PublicCookedFood.account.controller.BookmarkController;
import kwh.PublicCookedFood.account.controller.AccountBlockController;
import kwh.PublicCookedFood.account.controller.AccountController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.NoSuchElementException;

@Slf4j
@ControllerAdvice(assignableTypes = {
        MainController.class,
        RecipeController.class,
        BoardController.class,
        BoardDetailController.class,
        AccountController.class,
        AccountBlockController.class,
        BookmarkController.class,
        AdminBoardPageController.class
})
public class GlobalPageExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    public String handleNoSuchElement(NoSuchElementException e,
                                      HttpServletRequest request,
                                      RedirectAttributes redirectAttributes) {
        log.warn("페이지 리소스 조회 실패. uri={}, message={}", request.getRequestURI(), e.getMessage());
        redirectAttributes.addFlashAttribute("globalErrorCode", CommonErrorCode.RESOURCE_NOT_FOUND.code());
        redirectAttributes.addFlashAttribute("globalErrorMessage", CommonErrorCode.RESOURCE_NOT_FOUND.message());
        return "redirect:" + resolveRedirectPath(request.getRequestURI());
    }

    @ExceptionHandler(AppException.class)
    public String handleAppException(AppException e,
                                     HttpServletRequest request,
                                     RedirectAttributes redirectAttributes) {
        if (e.getErrorCode().status().is5xxServerError()) {
            log.error("페이지 요청 애플리케이션 예외. uri={}, code={}", request.getRequestURI(), e.getErrorCode().code(), e);
        } else {
            log.warn("페이지 요청 애플리케이션 예외. uri={}, code={}, message={}",
                    request.getRequestURI(), e.getErrorCode().code(), e.getMessage());
        }
        redirectAttributes.addFlashAttribute("globalErrorCode", e.getErrorCode().code());
        redirectAttributes.addFlashAttribute(
                "globalErrorMessage",
                ErrorMessageResolver.resolve(e.getMessage(), e.getErrorCode().message())
        );
        return "redirect:" + resolveRedirectPath(request.getRequestURI());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleInvalidInput(IllegalArgumentException e,
                                     HttpServletRequest request,
                                     RedirectAttributes redirectAttributes) {
        log.warn("페이지 요청 처리 실패. uri={}, message={}", request.getRequestURI(), e.getMessage());
        String message = ErrorMessageResolver.resolve(e.getMessage(), "요청을 처리할 수 없습니다.");
        redirectAttributes.addFlashAttribute("globalErrorCode", CommonErrorCode.INVALID_REQUEST.code());
        redirectAttributes.addFlashAttribute("globalErrorMessage", message);
        return "redirect:" + resolveRedirectPath(request.getRequestURI());
    }

    private String resolveRedirectPath(String requestUri) {
        if (requestUri == null || requestUri.isBlank()) {
            return "/recipes";
        }
        if (requestUri.startsWith("/boards")) {
            return "/boards";
        }
        if (requestUri.startsWith("/recipes")) {
            return "/recipes";
        }
        if (requestUri.startsWith("/bookmarks")) {
            return "/recipes";
        }
        if (requestUri.startsWith("/admin/boards/policy")) {
            return "/admin/boards/policy";
        }
        if (requestUri.startsWith("/admin/boards/dashboard")) {
            return "/admin/boards/dashboard";
        }
        if (requestUri.startsWith("/admin/boards/reports")) {
            return "/admin/boards/reports";
        }
        if (requestUri.startsWith("/admin/boards")) {
            return "/admin/boards/dashboard";
        }
        if (requestUri.matches("^/u/\\d+(/(comments|scraps))?$")) {
            return "/boards";
        }
        if (requestUri.startsWith("/u")) {
            return "/u/login";
        }
        return "/recipes";
    }
}
