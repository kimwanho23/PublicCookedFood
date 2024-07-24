package kwh.PublicCookedFood.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import kwh.PublicCookedFood.user.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;

// 커스텀 로그인 핸들러
@Component
public class CustomAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final String PREV_PAGE_ATTRIBUTE = "prevPage";
    private static final String LOGIN_PAGE_URI = "/u/login";
    private static final String REGISTER_PAGE_URI = "/u/signup";

    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    public CustomAuthenticationSuccessHandler() {
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        // 이전 페이지 가져오기
        String prevPage = (String) request.getSession().getAttribute(PREV_PAGE_ATTRIBUTE);
        if (prevPage != null) {
            request.getSession().removeAttribute(PREV_PAGE_ATTRIBUTE);
        }

        // 기본 URI
        String uri = determineRedirectUri(prevPage);
        redirectStrategy.sendRedirect(request, response, uri);
    }


    private String determineRedirectUri(String prevPage) {
        if (prevPage != null && !prevPage.isEmpty() && !prevPage.contains(LOGIN_PAGE_URI) && !prevPage.contains(REGISTER_PAGE_URI)) {
            return prevPage;
        }
        return "/";
    }

    protected void clearSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        }
    }
}