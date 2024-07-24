package kwh.PublicCookedFood.common;

import jakarta.servlet.http.HttpSession;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.context.request.WebRequest;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {

    private final UserService userService;

    @ModelAttribute
    public void addUserToModel(HttpSession session, WebRequest request, Model model) {
        Users user = userService.getUserFromSession(session); // 사용자 정보를 세션에 설정
        model.addAttribute("user", user);
        request.setAttribute("user", user, WebRequest.SCOPE_REQUEST);

    }
}