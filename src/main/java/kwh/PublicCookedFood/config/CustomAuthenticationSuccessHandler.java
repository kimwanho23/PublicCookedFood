package kwh.PublicCookedFood.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

// 커스텀 로그인 핸들러
@Component
public class CustomAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final String PREV_PAGE_ATTRIBUTE = "prevPage";
    private static final String LOGIN_PAGE_URI = "/u/login";
    private static final String REGISTER_PAGE_URI = "/u/signup";
    private static final String DEFAULT_REDIRECT_URI = "/foods";

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
        String uri = redirectUri(request, prevPage);
        redirectStrategy.sendRedirect(request, response, uri); //로그인 시 이전 url로 리다이렉트
    }

    private String redirectUri(HttpServletRequest request, String prevPage) {
        if (prevPage == null || prevPage.isEmpty()) {
            return DEFAULT_REDIRECT_URI;
        }

        try {
            URI uri = new URI(prevPage);
            if (uri.isAbsolute() && !request.getServerName().equalsIgnoreCase(uri.getHost())) {
                return DEFAULT_REDIRECT_URI;
            }

            String path = uri.getPath();
            if (path == null || !path.startsWith("/") || path.startsWith("//")) {
                return DEFAULT_REDIRECT_URI;
            }

            if (path.startsWith(LOGIN_PAGE_URI) || path.startsWith(REGISTER_PAGE_URI)) {
                return DEFAULT_REDIRECT_URI;
            }

            return uri.getQuery() == null ? path : path + "?" + uri.getQuery();
        } catch (URISyntaxException e) {
            return DEFAULT_REDIRECT_URI;
        }
    }
}
