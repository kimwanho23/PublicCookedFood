package kwh.PublicCookedFood.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class AuthThrottleService {

    private static final int LOGIN_MAX_ATTEMPTS = 5;
    private static final long LOGIN_WINDOW_MILLIS = TimeUnit.MINUTES.toMillis(5);
    private static final int RECOVERY_MAX_ATTEMPTS = 5;
    private static final long RECOVERY_WINDOW_MILLIS = TimeUnit.MINUTES.toMillis(10);
    private static final int AI_MAX_ATTEMPTS = 20;
    private static final long AI_WINDOW_MILLIS = TimeUnit.MINUTES.toMillis(1);
    private static final int CLEANUP_THRESHOLD = 2_000;

    private final Map<String, AttemptWindow> counters = new ConcurrentHashMap<>();

    public boolean allowLoginAttempt(HttpServletRequest request, String email) {
        cleanupIfNeeded();
        String key = loginKey(request, email);
        return !isBlocked(key, LOGIN_MAX_ATTEMPTS, LOGIN_WINDOW_MILLIS);
    }

    public void recordLoginFailure(HttpServletRequest request, String email) {
        cleanupIfNeeded();
        String key = loginKey(request, email);
        recordFailure(key, LOGIN_WINDOW_MILLIS);
    }

    public void clearLoginFailures(HttpServletRequest request, String email) {
        cleanupIfNeeded();
        String key = loginKey(request, email);
        counters.remove(key);
    }

    public long getLoginRetryAfterSeconds(HttpServletRequest request, String email) {
        cleanupIfNeeded();
        String key = loginKey(request, email);
        return getRetryAfterSeconds(key, LOGIN_MAX_ATTEMPTS, LOGIN_WINDOW_MILLIS);
    }

    public boolean tryConsumeRecoveryAttempt(HttpServletRequest request, String action, String identity) {
        cleanupIfNeeded();
        String key = recoveryKey(request, action, identity);
        return tryConsume(key, RECOVERY_MAX_ATTEMPTS, RECOVERY_WINDOW_MILLIS);
    }

    public long getRecoveryRetryAfterSeconds(HttpServletRequest request, String action, String identity) {
        cleanupIfNeeded();
        String key = recoveryKey(request, action, identity);
        return getRetryAfterSeconds(key, RECOVERY_MAX_ATTEMPTS, RECOVERY_WINDOW_MILLIS);
    }

    public boolean tryConsumeAiAttempt(HttpServletRequest request, String action) {
        cleanupIfNeeded();
        String key = aiKey(request, action);
        return tryConsume(key, AI_MAX_ATTEMPTS, AI_WINDOW_MILLIS);
    }

    public long getAiRetryAfterSeconds(HttpServletRequest request, String action) {
        cleanupIfNeeded();
        String key = aiKey(request, action);
        return getRetryAfterSeconds(key, AI_MAX_ATTEMPTS, AI_WINDOW_MILLIS);
    }

    private String loginKey(HttpServletRequest request, String email) {
        return "login:" + resolveClientIp(request) + ":" + normalizeText(email);
    }

    private String recoveryKey(HttpServletRequest request, String action, String identity) {
        return "recovery:" + normalizeText(action) + ":" + resolveClientIp(request) + ":" + normalizeText(identity);
    }

    private String aiKey(HttpServletRequest request, String action) {
        return "ai:" + normalizeText(action) + ":" + resolveClientIp(request);
    }

    private boolean tryConsume(String key, int maxAttempts, long windowMillis) {
        long now = System.currentTimeMillis();
        AttemptWindow window = counters.computeIfAbsent(key, k -> new AttemptWindow(now, 0));

        synchronized (window) {
            if (isExpired(window, now, windowMillis)) {
                window.reset(now, 1);
                return true;
            }
            if (window.attempts >= maxAttempts) {
                return false;
            }
            window.attempts++;
            return true;
        }
    }

    private void recordFailure(String key, long windowMillis) {
        long now = System.currentTimeMillis();
        AttemptWindow window = counters.computeIfAbsent(key, k -> new AttemptWindow(now, 0));

        synchronized (window) {
            if (isExpired(window, now, windowMillis)) {
                window.reset(now, 1);
                return;
            }
            window.attempts++;
        }
    }

    private boolean isBlocked(String key, int maxAttempts, long windowMillis) {
        AttemptWindow window = counters.get(key);
        if (window == null) {
            return false;
        }

        long now = System.currentTimeMillis();
        synchronized (window) {
            if (isExpired(window, now, windowMillis)) {
                counters.remove(key, window);
                return false;
            }
            return window.attempts >= maxAttempts;
        }
    }

    private long getRetryAfterSeconds(String key, int maxAttempts, long windowMillis) {
        AttemptWindow window = counters.get(key);
        if (window == null) {
            return 0L;
        }

        long now = System.currentTimeMillis();
        synchronized (window) {
            if (isExpired(window, now, windowMillis)) {
                counters.remove(key, window);
                return 0L;
            }
            if (window.attempts < maxAttempts) {
                return 0L;
            }
            long remainingMillis = windowMillis - (now - window.windowStartMillis);
            return Math.max(1L, (remainingMillis + 999L) / 1000L);
        }
    }

    private boolean isExpired(AttemptWindow window, long now, long windowMillis) {
        return now - window.windowStartMillis >= windowMillis;
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String remoteAddr = request.getRemoteAddr();
        return remoteAddr == null || remoteAddr.isBlank() ? "unknown" : remoteAddr.trim();
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "-";
        }
        String normalized = value.trim().toLowerCase();
        return normalized.isEmpty() ? "-" : normalized;
    }

    private void cleanupIfNeeded() {
        if (counters.size() < CLEANUP_THRESHOLD) {
            return;
        }
        long now = System.currentTimeMillis();
        long maxWindowMillis = Math.max(Math.max(LOGIN_WINDOW_MILLIS, RECOVERY_WINDOW_MILLIS), AI_WINDOW_MILLIS);
        counters.entrySet().removeIf(entry -> {
            AttemptWindow window = entry.getValue();
            return window == null || now - window.windowStartMillis >= maxWindowMillis;
        });
    }

    private static final class AttemptWindow {
        private long windowStartMillis;
        private int attempts;

        private AttemptWindow(long windowStartMillis, int attempts) {
            this.windowStartMillis = windowStartMillis;
            this.attempts = attempts;
        }

        private void reset(long windowStartMillis, int attempts) {
            this.windowStartMillis = windowStartMillis;
            this.attempts = attempts;
        }
    }
}
