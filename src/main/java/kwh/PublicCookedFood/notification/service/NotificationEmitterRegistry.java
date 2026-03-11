package kwh.PublicCookedFood.notification.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

final class NotificationEmitterRegistry {

    private final Map<Long, EmitterBucket> emittersByReceiver = new ConcurrentHashMap<>();
    private final AtomicLong emitterSequence = new AtomicLong();

    EmitterSession register(Long receiverId, long timeoutMs) {
        return emittersByReceiver.computeIfAbsent(receiverId, EmitterBucket::new)
                .register(emitterSequence.incrementAndGet(), timeoutMs);
    }

    List<EmitterSession> sessionsForReceiver(Long receiverId) {
        EmitterBucket emitterBucket = emittersByReceiver.get(receiverId);
        if (emitterBucket == null) {
            return List.of();
        }
        return emitterBucket.sessions();
    }

    List<EmitterSession> allSessions() {
        List<EmitterSession> sessions = new ArrayList<>(activeEmitterCount());
        emittersByReceiver.values().forEach(emitterBucket -> emitterBucket.appendSessionsTo(sessions));
        return sessions;
    }

    List<SseEmitter> clear(Long receiverId) {
        EmitterBucket emitterBucket = emittersByReceiver.remove(receiverId);
        if (emitterBucket == null) {
            return List.of();
        }
        return emitterBucket.clear();
    }

    void remove(Long receiverId, String emitterId) {
        EmitterBucket emitterBucket = emittersByReceiver.get(receiverId);
        if (emitterBucket == null) {
            return;
        }
        emitterBucket.remove(emitterId);
        if (emitterBucket.isEmpty()) {
            emittersByReceiver.remove(receiverId, emitterBucket);
        }
    }

    boolean isEmpty() {
        return emittersByReceiver.isEmpty();
    }

    int receiverCount() {
        return emittersByReceiver.size();
    }

    int activeEmitterCount() {
        return emittersByReceiver.values().stream()
                .mapToInt(EmitterBucket::size)
                .sum();
    }

    record EmitterSession(Long receiverId, String emitterId, SseEmitter emitter) {
    }

    private static final class EmitterBucket {

        private final Long receiverId;
        private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

        private EmitterBucket(Long receiverId) {
            this.receiverId = receiverId;
        }

        private EmitterSession register(long sequence, long timeoutMs) {
            String emitterId = receiverId + "_" + sequence;
            SseEmitter emitter = new SseEmitter(timeoutMs);
            emitters.put(emitterId, emitter);
            return new EmitterSession(receiverId, emitterId, emitter);
        }

        private List<EmitterSession> sessions() {
            List<EmitterSession> sessions = new ArrayList<>(emitters.size());
            appendSessionsTo(sessions);
            return sessions;
        }

        private void appendSessionsTo(List<EmitterSession> sessions) {
            emitters.forEach((emitterId, emitter) -> sessions.add(new EmitterSession(receiverId, emitterId, emitter)));
        }

        private List<SseEmitter> clear() {
            List<SseEmitter> removedEmitters = new ArrayList<>(emitters.values());
            emitters.clear();
            return removedEmitters;
        }

        private void remove(String emitterId) {
            emitters.remove(emitterId);
        }

        private boolean isEmpty() {
            return emitters.isEmpty();
        }

        private int size() {
            return emitters.size();
        }
    }
}
