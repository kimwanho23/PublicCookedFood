package kwh.PublicCookedFood.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
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
        String prevPage = (String) request.getSession().getAttribute(PREV_PAGE_ATTRIBUTE); // 이전 페이지를 가져온다.
        if (prevPage != null) {
            request.getSession().removeAttribute(PREV_PAGE_ATTRIBUTE);
        }

        // 기본 URI
        String uri = redirectUri(prevPage);
        redirectStrategy.sendRedirect(request, response, uri); //로그인 시 이전 url로 리다이렉트
    }

    private String redirectUri(String prevPage) {
        if (prevPage != null
                && !prevPage.isEmpty()
                && !prevPage.contains(LOGIN_PAGE_URI)
                && !prevPage.contains(REGISTER_PAGE_URI)) {
            return prevPage;
        }
        return "/";
    }
}