package kwh.PublicCookedFood.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class LoginThrottleFilter extends OncePerRequestFilter {

    private static final AntPathRequestMatcher LOGIN_REQUEST_MATCHER = new AntPathRequestMatcher("/u/login", "POST");

    private final AuthThrottleService authThrottleService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!LOGIN_REQUEST_MATCHER.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String email = request.getParameter("email");
        if (!authThrottleService.allowLoginAttempt(request, email)) {
            long retryAfter = authThrottleService.getLoginRetryAfterSeconds(request, email);
            response.sendRedirect("/u/login?error=locked&retryAfter=" + retryAfter);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
