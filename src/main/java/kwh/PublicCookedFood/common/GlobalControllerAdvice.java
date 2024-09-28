package kwh.PublicCookedFood.common;

import jakarta.servlet.http.HttpSession;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalControllerAdvice {

    private final UserService userService;

    private final HttpSession httpSession;

    @ModelAttribute
    public void addUserToModel(Model model) { // 현재 유저를 세션에 등록, 헤더 때문에 필요
        Users user = (Users) httpSession.getAttribute("user");

        if (user == null) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                String email = authentication.getName();
                user = userService.findUserByEmail(email);
                httpSession.setAttribute("user", user);
            }
        }
        model.addAttribute("user", user);
    }
}