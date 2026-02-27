package kwh.PublicCookedFood.user.controller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import kwh.PublicCookedFood.config.oauth2.LoginUser;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.facade.UserBlockFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;

@Controller
@RequiredArgsConstructor
@RequestMapping("/u/blocks")
@Slf4j
@Hidden
public class UserBlockController {

    private final UserBlockFacade userBlockFacade;

    @PostMapping("/{targetUserId}")
    public String block(@LoginUser Users user,
                        @PathVariable @Positive Long targetUserId,
                        HttpServletRequest request,
                        RedirectAttributes redirectAttributes) {
        if (user == null) {
            userBlockFacade.auditBlockFailedUnauthenticated(targetUserId);
            return "redirect:/u/login";
        }

        UserBlockFacade.BlockOperationResult operationResult = userBlockFacade.block(user.getId(), targetUserId);
        redirectAttributes.addFlashAttribute("blockMessage", operationResult.message());
        return redirectToReferer(request);
    }

    @PatchMapping("/{targetUserId}")
    public String unblock(@LoginUser Users user,
                          @PathVariable @Positive Long targetUserId,
                          HttpServletRequest request,
                          RedirectAttributes redirectAttributes) {
        if (user == null) {
            userBlockFacade.auditUnblockFailedUnauthenticated(targetUserId);
            return "redirect:/u/login";
        }

        UserBlockFacade.BlockOperationResult operationResult = userBlockFacade.unblock(user.getId(), targetUserId);
        redirectAttributes.addFlashAttribute("blockMessage", operationResult.message());
        return redirectToReferer(request);
    }

    private String redirectToReferer(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return "redirect:/boards";
        }

        try {
            URI uri = URI.create(referer);
            URI currentRequestUri = URI.create(request.getRequestURL().toString());
            if (uri.isAbsolute() && !isSameOrigin(currentRequestUri, uri)) {
                return "redirect:/boards";
            }
            String path = uri.getPath();
            if (path == null || path.isBlank() || !path.startsWith("/") || path.startsWith("//")) {
                return "redirect:/boards";
            }
            String query = uri.getQuery();
            if (query == null || query.isBlank()) {
                return "redirect:" + path;
            }
            return "redirect:" + path + "?" + query;
        } catch (IllegalArgumentException e) {
            log.debug("Invalid referer for block redirect. referer={}", referer, e);
            return "redirect:/boards";
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
