package kwh.PublicCookedFood.config;

import jakarta.servlet.http.HttpServletRequest;
import kwh.PublicCookedFood.board.contoller.AdminBoardPageController;
import kwh.PublicCookedFood.board.contoller.BoardController;
import kwh.PublicCookedFood.food.controller.MainController;
import kwh.PublicCookedFood.food.controller.RecipeController;
import kwh.PublicCookedFood.user.controller.BookmarkController;
import kwh.PublicCookedFood.user.controller.UserBlockController;
import kwh.PublicCookedFood.user.controller.UserController;
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
        UserController.class,
        UserBlockController.class,
        BookmarkController.class,
        AdminBoardPageController.class
})
public class GlobalPageExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    public String handleNoSuchElement(NoSuchElementException e,
                                      HttpServletRequest request,
                                      RedirectAttributes redirectAttributes) {
        log.warn("페이지 리소스 조회 실패. uri={}, message={}", request.getRequestURI(), e.getMessage());
        redirectAttributes.addFlashAttribute("globalErrorMessage", "요청한 리소스를 찾을 수 없습니다.");
        return "redirect:" + resolveRedirectPath(request.getRequestURI());
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public String handleInvalidInput(RuntimeException e,
                                     HttpServletRequest request,
                                     RedirectAttributes redirectAttributes) {
        log.warn("페이지 요청 처리 실패. uri={}, message={}", request.getRequestURI(), e.getMessage());
        String message = e.getMessage() == null || e.getMessage().isBlank()
                ? "요청을 처리할 수 없습니다."
                : e.getMessage();
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
        if (requestUri.startsWith("/admin/boards")) {
            return "/admin/boards/reports";
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
