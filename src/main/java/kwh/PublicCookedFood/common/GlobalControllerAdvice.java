package kwh.PublicCookedFood.common;

import jakarta.servlet.http.HttpSession;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.context.request.WebRequest;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {

    private final UserService userService;

    private final HttpSession httpSession;

    @ModelAttribute
    public void addUserToModel(@AuthenticationPrincipal OAuth2User oAuth2User, WebRequest request, Model model) {
        Users user = userService.getUserFromSession(httpSession); // 사용자 정보를 세션에 설정
        if (user == null && oAuth2User != null) {
            user = convertToUsers(oAuth2User);
        }
        model.addAttribute("user", user);
        request.setAttribute("user", user, WebRequest.SCOPE_REQUEST);

    }

    private Users convertToUsers(OAuth2User oAuth2User) {
        return Users.builder()
                .email(oAuth2User.getAttribute("email"))
                .name(oAuth2User.getAttribute("name"))
                .build();
    }


}