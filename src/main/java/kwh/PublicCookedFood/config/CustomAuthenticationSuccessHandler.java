package kwh.PublicCookedFood.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kwh.PublicCookedFood.user.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

// 커스텀 로그인 핸들러
@Component
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final String PREV_PAGE_ATTRIBUTE = "prevPage";
    private static final String LOGIN_PAGE_URI = "/u/login";
    private static final String REGISTER_PAGE_URI = "/u/signup";
    private static final String OAUTH2_AUTHORIZATION_URI = "/oauth2/authorization";
    private static final String DEFAULT_REDIRECT_URI = "/recipes";

    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
    private final RequestCache requestCache = new HttpSessionRequestCache();
    private final AuthThrottleService authThrottleService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        authThrottleService.clearLoginFailures(request, resolveLoginEmail(request, authentication));

        SavedRequest savedRequest = requestCache.getRequest(request, response);
        String savedRequestRedirectUrl = extractRedirectUrl(savedRequest);
        if (savedRequest != null) {
            requestCache.removeRequest(request, response);
        }

        String prevPage = (String) request.getSession().getAttribute(PREV_PAGE_ATTRIBUTE); // 이전 페이지를 가져온다.
        if (prevPage != null) {
            request.getSession().removeAttribute(PREV_PAGE_ATTRIBUTE);
        }

        String uri = redirectUri(request, savedRequestRedirectUrl, prevPage);
        redirectStrategy.sendRedirect(request, response, uri); //로그인 시 이전 url로 리다이렉트
    }

    private String resolveLoginEmail(HttpServletRequest request, Authentication authentication) {
        String requestedEmail = request.getParameter("email");
        if (requestedEmail != null && !requestedEmail.isBlank()) {
            return requestedEmail;
        }
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails customUserDetails
                && customUserDetails.getUser() != null) {
            return customUserDetails.getUser().getEmail();
        }
        return null;
    }

    private String extractRedirectUrl(SavedRequest savedRequest) {
        if (savedRequest == null) {
            return null;
        }

        String method = savedRequest.getMethod();
        if (method != null && !"GET".equalsIgnoreCase(method)) {
            return null;
        }
        return savedRequest.getRedirectUrl();
    }

    private String redirectUri(HttpServletRequest request, String... candidates) {
        for (String candidate : candidates) {
            String uri = safeRedirectUri(request, candidate);
            if (uri != null) {
                return uri;
            }
        }
        return DEFAULT_REDIRECT_URI;
    }

    private String safeRedirectUri(HttpServletRequest request, String rawUri) {
        if (rawUri == null || rawUri.isBlank()) {
            return null;
        }

        try {
            URI uri = new URI(rawUri);
            URI currentRequestUri = URI.create(request.getRequestURL().toString());
            if (uri.isAbsolute() && !isSameOrigin(currentRequestUri, uri)) {
                return null;
            }

            String path = uri.getPath();
            if (path == null || !path.startsWith("/") || path.startsWith("//")) {
                return null;
            }

            if (path.startsWith(LOGIN_PAGE_URI)
                    || path.startsWith(REGISTER_PAGE_URI)
                    || path.startsWith(OAUTH2_AUTHORIZATION_URI)
                    || path.startsWith("/error")) {
                return null;
            }

            return uri.getQuery() == null ? path : path + "?" + uri.getQuery();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private boolean isSameOrigin(URI source, URI target) {
        if (source == null || target == null) {
            return false;
        }
        if (source.getHost() == null || target.getHost() == null) {
            return false;
        }

        boolean sameScheme = source.getScheme() != null
                && source.getScheme().equalsIgnoreCase(target.getScheme());
        boolean sameHost = source.getHost().equalsIgnoreCase(target.getHost());
        boolean samePort = resolvePort(source) == resolvePort(target);

        return sameScheme && sameHost && samePort;
    }

    private int resolvePort(URI uri) {
        if (uri.getPort() != -1) {
            return uri.getPort();
        }
        if ("https".equalsIgnoreCase(uri.getScheme())) {
            return 443;
        }
        return 80;
    }
}
