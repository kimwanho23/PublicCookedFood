package kwh.PublicCookedFood.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class ClientIpResolver {

    private static final List<String> FORWARDED_IP_HEADERS = List.of(
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP"
    );

    public String resolve(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String remoteAddr = normalizeIpToken(request.getRemoteAddr());
        if (!isTrustedProxyAddress(remoteAddr)) {
            return remoteAddr == null ? "unknown" : remoteAddr;
        }

        for (String headerName : FORWARDED_IP_HEADERS) {
            String forwarded = extractFirstForwardedIp(request.getHeader(headerName));
            if (forwarded != null) {
                return forwarded;
            }
        }
        return remoteAddr == null ? "unknown" : remoteAddr;
    }

    private String extractFirstForwardedIp(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return null;
        }
        String[] candidates = headerValue.split(",");
        for (String candidate : candidates) {
            String normalized = normalizeIpToken(candidate);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String normalizeIpToken(String candidate) {
        if (candidate == null) {
            return null;
        }
        String normalized = candidate.trim();
        if (normalized.isBlank() || "unknown".equalsIgnoreCase(normalized)) {
            return null;
        }
        return normalized;
    }

    private boolean isTrustedProxyAddress(String ipAddress) {
        if (ipAddress == null) {
            return false;
        }
        String normalized = ipAddress.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("::ffff:")) {
            return isTrustedProxyAddress(normalized.substring("::ffff:".length()));
        }
        if ("127.0.0.1".equals(normalized)
                || "::1".equals(normalized)
                || "0:0:0:0:0:0:0:1".equals(normalized)) {
            return true;
        }
        if (normalized.startsWith("10.")
                || normalized.startsWith("192.168.")
                || normalized.startsWith("fc")
                || normalized.startsWith("fd")
                || normalized.startsWith("fe80:")) {
            return true;
        }
        if (!normalized.startsWith("172.")) {
            return false;
        }
        String[] parts = normalized.split("\\.");
        if (parts.length < 2) {
            return false;
        }
        try {
            int secondOctet = Integer.parseInt(parts[1]);
            return secondOctet >= 16 && secondOctet <= 31;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
