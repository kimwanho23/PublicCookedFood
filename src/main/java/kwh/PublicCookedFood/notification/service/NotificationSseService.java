package kwh.PublicCookedFood.notification.service;

import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.config.properties.NotificationSseProperties;
import kwh.PublicCookedFood.notification.error.NotificationErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationSseService {

    private static final long SSE_TIMEOUT_MS = 60L * 60L * 1000L;

    private final NotificationEmitterRegistry emitterRegistry = new NotificationEmitterRegistry();
    private final AtomicLong sseSendAttempts = new AtomicLong(0);
    private final AtomicLong sseSendSuccess = new AtomicLong(0);
    private final AtomicLong sseSendFailure = new AtomicLong(0);
    private final AtomicLong sseNotificationEvents = new AtomicLong(0);
    private final AtomicLong sseHeartbeatEvents = new AtomicLong(0);
    private final AtomicLong lastLoggedAttempts = new AtomicLong(0);
    private final AtomicLong lastLoggedFailures = new AtomicLong(0);
    private final NotificationSseProperties notificationSseProperties;

    public boolean isSseEnabled() {
        return notificationSseProperties.enabled();
    }

    public SseEmitter subscribe(Long receiverId) {
        if (!isSseEnabled()) {
            throw new AppException(NotificationErrorCode.NOTIFICATION_SSE_DISABLED);
        }
        NotificationEmitterRegistry.EmitterSession session = emitterRegistry.register(receiverId, SSE_TIMEOUT_MS);
        bindEmitterLifecycle(session);
        sendConnectedEvent(session);
        return session.emitter();
    }

    public void publishNotificationAfterCommit(Long receiverId, Long notificationId) {
        runAfterCommitOrNow(() -> sendNotificationEvent(receiverId, notificationId));
    }

    public void clearEmittersAfterCommit(Long receiverId) {
        runAfterCommitOrNow(() -> clearEmitters(receiverId));
    }

    public void clearEmitters(Long receiverId) {
        emitterRegistry.clear(receiverId).forEach(emitter -> {
            try {
                emitter.complete();
            } catch (IllegalStateException ignored) {
            }
        });
    }

    @Scheduled(fixedDelayString = "${app.notification.sse.heartbeat-interval-ms}")
    public void sendHeartbeat() {
        if (!isSseEnabled() || emitterRegistry.isEmpty()) {
            return;
        }
        sseHeartbeatEvents.incrementAndGet();
        NotificationHeartbeatEvent payload = NotificationHeartbeatEvent.now();
        for (NotificationEmitterRegistry.EmitterSession session : emitterRegistry.allSessions()) {
            sendToEmitter(session, "heartbeat", payload);
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
                emitterRegistry.receiverCount(),
                attempts,
                failures,
                formatRate(totalFailureRate),
                deltaAttempts,
                deltaFailures,
                formatRate(intervalFailureRate),
                sseNotificationEvents.get(),
                sseHeartbeatEvents.get());
    }

    private void bindEmitterLifecycle(NotificationEmitterRegistry.EmitterSession session) {
        session.emitter().onCompletion(() -> emitterRegistry.remove(session.receiverId(), session.emitterId()));
        session.emitter().onTimeout(() -> emitterRegistry.remove(session.receiverId(), session.emitterId()));
        session.emitter().onError(error -> emitterRegistry.remove(session.receiverId(), session.emitterId()));
    }

    private void sendConnectedEvent(NotificationEmitterRegistry.EmitterSession session) {
        sendToEmitter(session, "connected", NotificationConnectedEvent.now());
    }

    private void sendNotificationEvent(Long receiverId, Long notificationId) {
        var sessions = emitterRegistry.sessionsForReceiver(receiverId);
        if (sessions.isEmpty()) {
            return;
        }
        sseNotificationEvents.incrementAndGet();
        NotificationPublishedEvent payload = NotificationPublishedEvent.of(notificationId);
        for (NotificationEmitterRegistry.EmitterSession session : sessions) {
            sendToEmitter(session, "notification", payload);
        }
    }

    private void sendToEmitter(NotificationEmitterRegistry.EmitterSession session,
                               String eventName,
                               Object data) {
        sseSendAttempts.incrementAndGet();
        try {
            session.emitter().send(SseEmitter.event()
                    .id(session.emitterId() + ":" + System.currentTimeMillis())
                    .name(eventName)
                    .data(data));
            sseSendSuccess.incrementAndGet();
        } catch (IOException | IllegalStateException e) {
            sseSendFailure.incrementAndGet();
            emitterRegistry.remove(session.receiverId(), session.emitterId());
            log.debug("Failed to send SSE event. receiverId={}, emitterId={}, eventName={}",
                    session.receiverId(), session.emitterId(), eventName, e);
        }
    }

    private int getActiveEmitterCount() {
        return emitterRegistry.activeEmitterCount();
    }

    private void runAfterCommitOrNow(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
            return;
        }
        task.run();
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

    private record NotificationConnectedEvent(long timestamp) {

        private static NotificationConnectedEvent now() {
            return new NotificationConnectedEvent(System.currentTimeMillis());
        }
    }

    private record NotificationHeartbeatEvent(long timestamp) {

        private static NotificationHeartbeatEvent now() {
            return new NotificationHeartbeatEvent(System.currentTimeMillis());
        }
    }

    private record NotificationPublishedEvent(Long notificationId, long timestamp) {

        private static NotificationPublishedEvent of(Long notificationId) {
            return new NotificationPublishedEvent(notificationId, System.currentTimeMillis());
        }
    }
}
