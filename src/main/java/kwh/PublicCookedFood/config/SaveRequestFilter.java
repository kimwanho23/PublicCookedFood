package kwh.PublicCookedFood.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SaveRequestFilter extends OncePerRequestFilter {

    private static final String HOME_PAGE_URI = "/foods";
    private static final String LOGIN_PAGE_URI = "/u/login";
    private static final String SIGNUP_PAGE_URI = "/u/signup";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            if (new AntPathRequestMatcher(LOGIN_PAGE_URI).matches(request) || new AntPathRequestMatcher(SIGNUP_PAGE_URI).matches(request)) {
                response.sendRedirect(HOME_PAGE_URI);
                return;
            }
        }
/*        if (request.getMethod().equalsIgnoreCase("GET") &&
                !new AntPathRequestMatcher(LOGIN_PAGE_URI).matches(request) &&
                !new AntPathRequestMatcher(REGISTER_PAGE_URI).matches(request)) {
            String referrer = request.getHeader("Referer");

            if (referrer != null && !referrer.contains(LOGIN_PAGE_URI) && !referrer.contains(REGISTER_PAGE_URI)) {
                request.getSession().setAttribute(PREV_PAGE_ATTRIBUTE, referrer);
            }
        }*/
        filterChain.doFilter(request, response);
    }
}