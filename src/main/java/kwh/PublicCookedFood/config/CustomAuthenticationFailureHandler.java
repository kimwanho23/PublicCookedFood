package kwh.PublicCookedFood.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final AuthThrottleService authThrottleService;
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String email = request.getParameter("email");
        authThrottleService.recordLoginFailure(request, email);

        if (!authThrottleService.allowLoginAttempt(request, email)) {
            long retryAfter = authThrottleService.getLoginRetryAfterSeconds(request, email);
            redirectStrategy.sendRedirect(request, response, "/u/login?error=locked&retryAfter=" + retryAfter);
            return;
        }

        redirectStrategy.sendRedirect(request, response, "/u/login?error=true");
    }
}
