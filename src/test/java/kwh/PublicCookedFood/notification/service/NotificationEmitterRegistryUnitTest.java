package kwh.PublicCookedFood.notification.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class NotificationEmitterRegistryUnitTest {

    @Test
    void missingReceiver_usesEmptyEmitterBucketContract() {
        NotificationEmitterRegistry registry = new NotificationEmitterRegistry();

        assertThat(registry.sessionsForReceiver(99L)).isEmpty();
        assertThat(registry.clear(99L)).isEmpty();
        assertThatCode(() -> registry.remove(99L, "missing")).doesNotThrowAnyException();
        assertThat(registry.isEmpty()).isTrue();
    }

    @Test
    void register_tracksActiveEmittersPerReceiver() {
        NotificationEmitterRegistry registry = new NotificationEmitterRegistry();

        NotificationEmitterRegistry.EmitterSession first = registry.register(1L, 1_000L);
        NotificationEmitterRegistry.EmitterSession second = registry.register(1L, 1_000L);
        NotificationEmitterRegistry.EmitterSession third = registry.register(2L, 1_000L);

        assertThat(first.receiverId()).isEqualTo(1L);
        assertThat(second.receiverId()).isEqualTo(1L);
        assertThat(third.receiverId()).isEqualTo(2L);
        assertThat(registry.activeEmitterCount()).isEqualTo(3);
        assertThat(registry.receiverCount()).isEqualTo(2);
        assertThat(registry.sessionsForReceiver(1L)).hasSize(2);
        assertThat(registry.sessionsForReceiver(2L)).hasSize(1);
    }

    @Test
    void remove_dropsReceiverBucketWhenLastEmitterIsRemoved() {
        NotificationEmitterRegistry registry = new NotificationEmitterRegistry();
        NotificationEmitterRegistry.EmitterSession session = registry.register(7L, 1_000L);

        registry.remove(session.receiverId(), session.emitterId());

        assertThat(registry.sessionsForReceiver(7L)).isEmpty();
        assertThat(registry.activeEmitterCount()).isZero();
        assertThat(registry.receiverCount()).isZero();
        assertThat(registry.isEmpty()).isTrue();
    }

    @Test
    void clear_returnsRegisteredEmittersAndRemovesReceiverEntries() {
        NotificationEmitterRegistry registry = new NotificationEmitterRegistry();
        NotificationEmitterRegistry.EmitterSession first = registry.register(9L, 1_000L);
        NotificationEmitterRegistry.EmitterSession second = registry.register(9L, 1_000L);
        registry.register(11L, 1_000L);

        List<SseEmitter> cleared = registry.clear(9L);

        assertThat(cleared).containsExactlyInAnyOrder(first.emitter(), second.emitter());
        assertThat(registry.sessionsForReceiver(9L)).isEmpty();
        assertThat(registry.activeEmitterCount()).isEqualTo(1);
        assertThat(registry.receiverCount()).isEqualTo(1);
    }
}
