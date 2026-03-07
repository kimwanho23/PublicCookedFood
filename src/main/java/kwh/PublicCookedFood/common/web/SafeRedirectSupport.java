package kwh.PublicCookedFood.common.web;

import jakarta.servlet.http.HttpServletRequest;

import java.net.URI;

public final class SafeRedirectSupport {

    private SafeRedirectSupport() {
    }

    public static String normalizeRelativePath(String redirect) {
        if (redirect == null || redirect.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(redirect.trim());
            return normalizePathAndQuery(uri, false);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static String toRedirectOrDefault(String redirect, String fallbackPath) {
        String safePath = normalizeRelativePath(redirect);
        return toRedirect(safePath == null ? fallbackPath : safePath);
    }

    public static String resolveRefererRedirect(HttpServletRequest request, String fallbackPath) {
        if (request == null) {
            return toRedirect(fallbackPath);
        }

        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return toRedirect(fallbackPath);
        }

        try {
            URI refererUri = URI.create(referer.trim());
            URI currentRequestUri = URI.create(request.getRequestURL().toString());
            if (refererUri.isAbsolute() && !isSameOrigin(currentRequestUri, refererUri)) {
                return toRedirect(fallbackPath);
            }

            String safePath = normalizePathAndQuery(refererUri, true);
            return toRedirect(safePath == null ? fallbackPath : safePath);
        } catch (IllegalArgumentException e) {
            return toRedirect(fallbackPath);
        }
    }

    public static String toRedirect(String path) {
        return "redirect:" + path;
    }

    private static String normalizePathAndQuery(URI uri, boolean allowAbsoluteUri) {
        if (uri == null) {
            return null;
        }
        if (!allowAbsoluteUri && uri.isAbsolute()) {
            return null;
        }

        String path = uri.getPath();
        if (path == null || path.isBlank() || !path.startsWith("/") || path.startsWith("//")) {
            return null;
        }

        StringBuilder safePath = new StringBuilder(path);
        String query = uri.getRawQuery();
        if (query != null && !query.isBlank()) {
            safePath.append('?').append(query);
        }
        String fragment = uri.getRawFragment();
        if (fragment != null && !fragment.isBlank()) {
            safePath.append('#').append(fragment);
        }
        return safePath.toString();
    }

    private static boolean isSameOrigin(URI source, URI target) {
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

    private static int resolvePort(URI uri) {
        if (uri.getPort() != -1) {
            return uri.getPort();
        }
        if ("https".equalsIgnoreCase(uri.getScheme())) {
            return 443;
        }
        return 80;
    }
}
