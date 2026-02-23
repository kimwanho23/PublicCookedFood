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
        String redirectUrl = "/foods";

        if (referer != null && !referer.isBlank()) {
            try {
                URI uri = new URI(referer);
                if (uri.isAbsolute() && !request.getServerName().equalsIgnoreCase(uri.getHost())) {
                    response.sendRedirect("/foods");
                    return;
                }

                String path = uri.getPath();
                if (path != null && path.startsWith("/") && !path.startsWith("//")) {
                    redirectUrl = "/u/profile".equals(path)
                            ? "/foods"
                            : (uri.getQuery() == null ? path : path + "?" + uri.getQuery());
                }
            } catch (URISyntaxException ignored) {
                redirectUrl = "/foods";
            }
        }

        response.sendRedirect(redirectUrl);
    }
}
