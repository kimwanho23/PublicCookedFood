package kwh.PublicCookedFood.user.controller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import kwh.PublicCookedFood.config.oauth2.LoginUser;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.service.UserActivityLogService;
import kwh.PublicCookedFood.user.service.UserBlockService;
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

    private final UserBlockService userBlockService;

    private final UserActivityLogService userActivityLogService;

    @PostMapping("/{targetUserId}")
    public String block(@LoginUser Users user,
                        @PathVariable @Positive Long targetUserId,
                        HttpServletRequest request,
                        RedirectAttributes redirectAttributes) {
        if (user == null) {
            log.warn("action=user.block result=failed reason=unauthenticated targetUserId={}", targetUserId);
            return "redirect:/u/login";
        }

        try {
            boolean created = userBlockService.block(user.getId(), targetUserId);
            if (created) {
                redirectAttributes.addFlashAttribute("blockMessage", "사용자를 차단했습니다.");
                log.info("action=user.block result=success blockerId={} blockedId={}",
                        user.getId(), targetUserId);
                userActivityLogService.record(
                        user.getId(),
                        "USER_BLOCK_ADD",
                        "targetUserId=" + targetUserId
                );
            } else {
                redirectAttributes.addFlashAttribute("blockMessage", "이미 차단된 사용자입니다.");
                log.info("action=user.block result=skipped_duplicate blockerId={} blockedId={}",
                        user.getId(), targetUserId);
            }
        } catch (RuntimeException e) {
            String message = e.getMessage() == null || e.getMessage().isBlank()
                    ? "차단 처리 중 오류가 발생했습니다."
                    : e.getMessage();
            redirectAttributes.addFlashAttribute("blockMessage", message);
            log.warn("action=user.block result=failed blockerId={} blockedId={} reason={}",
                    user.getId(), targetUserId, e.getMessage(), e);
        }
        return redirectToReferer(request);
    }

    @PatchMapping("/{targetUserId}")
    public String unblock(@LoginUser Users user,
                          @PathVariable @Positive Long targetUserId,
                          HttpServletRequest request,
                          RedirectAttributes redirectAttributes) {
        if (user == null) {
            log.warn("action=user.unblock result=failed reason=unauthenticated targetUserId={}", targetUserId);
            return "redirect:/u/login";
        }

        try {
            boolean removed = userBlockService.unblock(user.getId(), targetUserId);
            if (removed) {
                redirectAttributes.addFlashAttribute("blockMessage", "사용자 차단을 해제했습니다.");
                log.info("action=user.unblock result=success blockerId={} blockedId={}",
                        user.getId(), targetUserId);
                userActivityLogService.record(
                        user.getId(),
                        "USER_BLOCK_REMOVE",
                        "targetUserId=" + targetUserId
                );
            } else {
                redirectAttributes.addFlashAttribute("blockMessage", "차단 내역이 없어 변경하지 않았습니다.");
                log.info("action=user.unblock result=skipped_not_found blockerId={} blockedId={}",
                        user.getId(), targetUserId);
            }
        } catch (RuntimeException e) {
            String message = e.getMessage() == null || e.getMessage().isBlank()
                    ? "차단 해제 처리 중 오류가 발생했습니다."
                    : e.getMessage();
            redirectAttributes.addFlashAttribute("blockMessage", message);
            log.warn("action=user.unblock result=failed blockerId={} blockedId={} reason={}",
                    user.getId(), targetUserId, e.getMessage(), e);
        }
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
        } catch (RuntimeException e) {
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
