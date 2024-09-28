package kwh.PublicCookedFood.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@Component
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
                                Authentication authentication) throws IOException, ServletException {
        String referer = request.getHeader("Referer");
        String redirectUrl;

        if (referer != null) {
            try {
                URI uri = new URI(referer);
                String path = uri.getPath();

                // path가 /u/profile일 때만 /foods로 리다이렉트
                redirectUrl = "/u/profile".equals(path) ? "/foods" : referer;
            } catch (URISyntaxException e) {
                // URL이 잘못된 경우 기본 페이지로 리다이렉트
                redirectUrl = "/";
            }
        } else {
            // referer가 null인 경우 기본 페이지로 리다이렉트
            redirectUrl = "/";
        }

        response.sendRedirect(redirectUrl);
    }
}