package kwh.PublicCookedFood.board.controller.support;

import jakarta.servlet.http.HttpServletRequest;
import kwh.PublicCookedFood.board.service.comment.CommentPageSpec;
import kwh.PublicCookedFood.common.web.SafeRedirectSupport;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class BoardDetailNavigationSupport {

    public String resolveSafeRedirect(String redirect, long boardId) {
        return SafeRedirectSupport.toRedirectOrDefault(redirect, "/boards/" + boardId);
    }

    public boolean returnsToBoardDetail(String redirect, long boardId) {
        Optional<String> resolvedPath = SafeRedirectSupport.normalizeRelativePath(redirect);
        return resolvedPath.map(s -> isBoardDetailPath(s, boardId)).orElse(true);
    }

    public String commentPageRedirect(long boardId, int commentPage, int commentSize) {
        String targetPath = buildCommentPagePath(boardId, commentPage, commentSize);
        return "redirect:" + targetPath;
    }

    public Pageable resolveCommentPageable(int commentPage, int commentSize) {
        return new CommentPageSpec(commentPage, commentSize).toPageable();
    }

    private String buildCommentPagePath(long boardId, int commentPage, int commentSize) {
        Pageable pageable = resolveCommentPageable(commentPage, commentSize);
        String suffix = "#board-comments";
        return "/boards/" + boardId
                + "?commentPage=" + pageable.getPageNumber()
                + "&commentSize=" + pageable.getPageSize()
                + suffix;
    }

    private boolean isBoardDetailPath(String path, long boardId) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }
        try {
            return ("/boards/" + boardId).equals(URI.create(path).getPath());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isCommentPageNavigation(HttpServletRequest request, long boardId) {
        if (request == null) {
            return false;
        }
        if (request.getParameter("commentPage") == null && request.getParameter("commentSize") == null) {
            return false;
        }

        Optional<URI> refererUri = parseUri(request.getHeader("Referer"));
        if (refererUri.isEmpty()) {
            return false;
        }
        String expectedPath = "/boards/" + boardId;
        if (!expectedPath.equals(refererUri.get().getPath()) || !expectedPath.equals(request.getRequestURI())) {
            return false;
        }
        String refererHost = refererUri.get().getHost();
        if (refererHost != null && !refererHost.equalsIgnoreCase(request.getServerName())) {
            return false;
        }

        String currentCommentPage = normalizeQueryValue(request.getParameter("commentPage"));
        String currentCommentSize = normalizeQueryValue(request.getParameter("commentSize"));
        UriQueryParameters previousQueryParameters = UriQueryParameters.from(refererUri.get());
        String previousCommentPage = normalizeQueryValue(previousQueryParameters.value("commentPage"));
        String previousCommentSize = normalizeQueryValue(previousQueryParameters.value("commentSize"));
        return !currentCommentPage.equals(previousCommentPage)
                || !currentCommentSize.equals(previousCommentSize);
    }

    private Optional<URI> parseUri(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new URI(value));
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
    }

    private String normalizeQueryValue(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class UriQueryParameters {

        private static final UriQueryParameters EMPTY = new UriQueryParameters(Collections.<String, String>emptyMap());

        private final Map<String, String> values;

        private UriQueryParameters(Map<String, String> values) {
            this.values = values;
        }

        private static UriQueryParameters from(URI uri) {
            if (uri == null || uri.getQuery() == null || uri.getQuery().trim().isEmpty()) {
                return EMPTY;
            }
            Map<String, String> parameters = Arrays.stream(uri.getQuery().split("&"))
                    .map(token -> token.split("=", 2))
                    .filter(parts -> parts.length > 0 && !parts[0].trim().isEmpty())
                    .collect(java.util.stream.Collectors.toMap(
                            parts -> parts[0],
                            parts -> parts.length == 2 ? parts[1] : "",
                            (left, right) -> left,
                            LinkedHashMap::new
                    ));
            return new UriQueryParameters(Collections.unmodifiableMap(new LinkedHashMap<String, String>(parameters)));
        }

        private String value(String key) {
            if (key == null || key.trim().isEmpty()) {
                return "";
            }
            return values.getOrDefault(key, "");
        }
    }
}
