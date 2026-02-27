package kwh.PublicCookedFood.user.facade;

import jakarta.servlet.http.HttpServletRequest;
import kwh.PublicCookedFood.user.audit.UserAuditPublisher;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.dto.CustomUserDetails;
import kwh.PublicCookedFood.user.dto.request.UserSaveDto;
import kwh.PublicCookedFood.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;

@Service
@RequiredArgsConstructor
public class UserAuthFacade {

    private static final String DEFAULT_LOGIN_ERROR_MESSAGE = "아이디 또는 비밀번호를 확인해주세요.";
    private static final String OAUTH_LOGIN_ERROR_MESSAGE = "소셜 로그인에 실패했습니다. OAuth 클라이언트 설정(redirect URI, client id/secret)을 확인해주세요.";

    private final UserService userService;
    private final UserAuditPublisher userAuditPublisher;
    private final PasswordEncoder passwordEncoder;

    public void rememberPreviousPage(HttpServletRequest request, String currentPath) {
        if (request == null) {
            return;
        }
        String referer = request.getHeader("Referer");
        if (referer != null && (currentPath == null || !referer.contains(currentPath))) {
            request.getSession().setAttribute("prevPage", referer);
        }
    }

    @Transactional
    public SignupResult signup(UserSaveDto userSaveDto) {
        try {
            Users users = Users.createUser(userSaveDto, passwordEncoder);
            Users savedUser = userService.save(users);
            userAuditPublisher.userSignup(savedUser.getId(), savedUser.getEmail());
            return SignupResult.succeeded();
        } catch (IllegalStateException e) {
            return SignupResult.failed(e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public void markProfileUpdateFailedUnauthenticated() {
        userAuditPublisher.userProfileUpdateFailedUnauthenticated();
    }

    @Transactional(readOnly = true)
    public void markProfileUpdateFailedValidation(Long userId) {
        userAuditPublisher.userProfileUpdateFailedValidation(userId);
    }

    public void populateLoginErrorMessage(Model model,
                                          String error,
                                          Long retryAfter,
                                          Boolean oauthError) {
        if (error != null) {
            if ("locked".equalsIgnoreCase(error)) {
                long seconds = retryAfter == null ? 0L : retryAfter;
                model.addAttribute("loginErrorMsg",
                        seconds > 0
                                ? "로그인 시도가 너무 많습니다. 약 " + seconds + "초 후 다시 시도해주세요."
                                : "로그인 시도가 너무 많습니다. 잠시 후 다시 시도해주세요.");
            } else {
                model.addAttribute("loginErrorMsg", DEFAULT_LOGIN_ERROR_MESSAGE);
            }
        }
        if (Boolean.TRUE.equals(oauthError)) {
            model.addAttribute("loginErrorMsg", OAUTH_LOGIN_ERROR_MESSAGE);
        }
    }

    public void applyDefaultLoginError(Model model) {
        model.addAttribute("loginErrorMsg", DEFAULT_LOGIN_ERROR_MESSAGE);
    }

    public void refreshAuthenticationPrincipal(Users savedUser) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails currentPrincipal)) {
            return;
        }

        CustomUserDetails updatedPrincipal = currentPrincipal.getAttributes() == null
                ? new CustomUserDetails(savedUser)
                : new CustomUserDetails(savedUser, currentPrincipal.getAttributes());

        UsernamePasswordAuthenticationToken updatedAuthentication =
                new UsernamePasswordAuthenticationToken(
                        updatedPrincipal,
                        authentication.getCredentials(),
                        updatedPrincipal.getAuthorities());
        updatedAuthentication.setDetails(authentication.getDetails());
        SecurityContextHolder.getContext().setAuthentication(updatedAuthentication);
    }

    public record SignupResult(boolean success,
                               String errorMessage) {

        public static SignupResult succeeded() {
            return new SignupResult(true, null);
        }

        public static SignupResult failed(String errorMessage) {
            return new SignupResult(false, errorMessage);
        }
    }
}
