package kwh.PublicCookedFood.notification.service;

import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.config.properties.NotificationSseProperties;
import kwh.PublicCookedFood.notification.dto.response.NotificationResponse;
import kwh.PublicCookedFood.notification.error.NotificationErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationSseService {

    private static final long SSE_TIMEOUT_MS = 60L * 60L * 1000L;

    private final NotificationViewSupport notificationViewSupport;
    private final Map<Long, Map<String, SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final AtomicLong sseSendAttempts = new AtomicLong(0);
    private final AtomicLong sseSendSuccess = new AtomicLong(0);
    private final AtomicLong sseSendFailure = new AtomicLong(0);
    private final AtomicLong sseNotificationEvents = new AtomicLong(0);
    private final AtomicLong sseHeartbeatEvents = new AtomicLong(0);
    private final AtomicLong lastLoggedAttempts = new AtomicLong(0);
    private final AtomicLong lastLoggedFailures = new AtomicLong(0);
    private final NotificationSseProperties notificationSseProperties;

    public boolean isSseEnabled() {
        return Boolean.TRUE.equals(notificationSseProperties.enabled());
    }

    public SseEmitter subscribe(Long receiverId) {
        if (!isSseEnabled()) {
            throw new AppException(NotificationErrorCode.NOTIFICATION_SSE_DISABLED);
        }
        String emitterId = receiverId + "_" + System.currentTimeMillis();
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitters.computeIfAbsent(receiverId, key -> new ConcurrentHashMap<>()).put(emitterId, emitter);
        emitter.onCompletion(() -> removeEmitter(receiverId, emitterId));
        emitter.onTimeout(() -> removeEmitter(receiverId, emitterId));
        emitter.onError(error -> removeEmitter(receiverId, emitterId));
        sendConnectedEvent(receiverId, emitterId, emitter);
        return emitter;
    }

    public void publishNotificationAfterCommit(Long receiverId, Long notificationId) {
        Runnable publishTask = () -> sendNotificationEvent(receiverId, notificationId);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishTask.run();
                }
            });
            return;
        }
        publishTask.run();
    }

    public void clearEmitters(Long receiverId) {
        Map<String, SseEmitter> accountEmitters = emitters.remove(receiverId);
        if (accountEmitters == null) {
            return;
        }
        accountEmitters.values().forEach(emitter -> {
            try {
                emitter.complete();
            } catch (IllegalStateException ignored) {
            }
        });
    }

    @Scheduled(fixedDelayString = "${app.notification.sse.heartbeat-interval-ms}")
    public void sendHeartbeat() {
        if (!isSseEnabled() || emitters.isEmpty()) {
            return;
        }
        sseHeartbeatEvents.incrementAndGet();
        Map<String, Object> payload = Map.of("timestamp", System.currentTimeMillis());
        for (Map.Entry<Long, Map<String, SseEmitter>> accountEntry : emitters.entrySet()) {
            Long receiverId = accountEntry.getKey();
            for (Map.Entry<String, SseEmitter> emitterEntry : accountEntry.getValue().entrySet()) {
                sendToEmitter(receiverId, emitterEntry.getKey(), emitterEntry.getValue(), "heartbeat", payload);
            }
        }
    }

    @Scheduled(fixedDelayString = "${app.notification.sse.metrics-log-interval-ms}")
    public void logSseMetrics() {
        if (!isSseEnabled()) {
            return;
        }
        long attempts = sseSendAttempts.get();
        long failures = sseSendFailure.get();
        long deltaAttempts = attempts - lastLoggedAttempts.getAndSet(attempts);
        long deltaFailures = failures - lastLoggedFailures.getAndSet(failures);
        double totalFailureRate = calculateFailureRate(failures, attempts);
        double intervalFailureRate = calculateFailureRate(deltaFailures, deltaAttempts);

        log.info("action=notification.sse_metrics result=snapshot activeConnections={} receivers={} attempts={} failures={} failureRate={}%, " +
                        "intervalAttempts={} intervalFailures={} intervalFailureRate={}% notificationEvents={} heartbeatEvents={}",
                getActiveEmitterCount(),
                emitters.size(),
                attempts,
                failures,
                formatRate(totalFailureRate),
                deltaAttempts,
                deltaFailures,
                formatRate(intervalFailureRate),
                sseNotificationEvents.get(),
                sseHeartbeatEvents.get());
    }

    private void sendConnectedEvent(Long receiverId, String emitterId, SseEmitter emitter) {
        sendToEmitter(receiverId, emitterId, emitter, "connected",
                Map.of(
                        "timestamp", System.currentTimeMillis(),
                        "unreadCount", notificationViewSupport.countVisibleUnreadNotifications(receiverId)
                ));
    }

    private void sendNotificationEvent(Long receiverId, Long notificationId) {
        Map<String, SseEmitter> accountEmitters = emitters.get(receiverId);
        if (accountEmitters == null || accountEmitters.isEmpty()) {
            return;
        }
        sseNotificationEvents.incrementAndGet();
        long unreadCount = notificationViewSupport.countVisibleUnreadNotifications(receiverId);
        NotificationResponse latest = notificationViewSupport.loadVisibleNotification(receiverId, notificationId);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("unreadCount", unreadCount);
        payload.put("latest", latest);
        payload.put("timestamp", System.currentTimeMillis());

        for (Map.Entry<String, SseEmitter> entry : accountEmitters.entrySet()) {
            sendToEmitter(receiverId, entry.getKey(), entry.getValue(), "notification", payload);
        }
    }

    private void sendToEmitter(Long receiverId,
                               String emitterId,
                               SseEmitter emitter,
                               String eventName,
                               Object data) {
        sseSendAttempts.incrementAndGet();
        try {
            emitter.send(SseEmitter.event()
                    .id(emitterId + ":" + System.currentTimeMillis())
                    .name(eventName)
                    .data(data));
            sseSendSuccess.incrementAndGet();
        } catch (IOException | IllegalStateException e) {
            sseSendFailure.incrementAndGet();
            removeEmitter(receiverId, emitterId);
            log.debug("Failed to send SSE event. receiverId={}, emitterId={}, eventName={}", receiverId, emitterId, eventName, e);
        }
    }

    private void removeEmitter(Long receiverId, String emitterId) {
        Map<String, SseEmitter> accountEmitters = emitters.get(receiverId);
        if (accountEmitters == null) {
            return;
        }
        accountEmitters.remove(emitterId);
        if (accountEmitters.isEmpty()) {
            emitters.remove(receiverId);
        }
    }

    private int getActiveEmitterCount() {
        return emitters.values().stream()
                .filter(Objects::nonNull)
                .mapToInt(Map::size)
                .sum();
    }

    private double calculateFailureRate(long failures, long attempts) {
        if (attempts <= 0) {
            return 0.0d;
        }
        return (failures * 100.0d) / attempts;
    }

    private String formatRate(double rate) {
        return String.format("%.2f", rate);
    }
}
