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
        String redirectUrl = "/recipes";

        if (referer != null && !referer.isBlank()) {
            try {
                URI uri = new URI(referer);
                URI currentRequestUri = URI.create(request.getRequestURL().toString());
                if (uri.isAbsolute() && !isSameOrigin(currentRequestUri, uri)) {
                    response.sendRedirect("/recipes");
                    return;
                }

                String path = uri.getPath();
                if (path != null && path.startsWith("/") && !path.startsWith("//")) {
                    redirectUrl = ("/u/profile".equals(path) || "/u/settings".equals(path))
                            ? "/recipes"
                            : (uri.getQuery() == null ? path : path + "?" + uri.getQuery());
                }
            } catch (URISyntaxException ignored) {
                redirectUrl = "/recipes";
            }
        }

        response.sendRedirect(redirectUrl);
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
