package kwh.PublicCookedFood.notification.service;

import kwh.PublicCookedFood.config.properties.NotificationSseProperties;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationSseServiceUnitTest {

    @Test
    void clearEmittersAfterCommit_defersRegistryCleanupUntilCommit() {
        NotificationSseService service = new NotificationSseService(new NotificationSseProperties(true, 1L, 1L));
        service.subscribe(1L);
        NotificationEmitterRegistry registry = emitterRegistry(service);

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.clearEmittersAfterCommit(1L);

            assertThat(registry.activeEmitterCount()).isEqualTo(1);
            assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);

            triggerAfterCommit();

            assertThat(registry.activeEmitterCount()).isZero();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void publishNotificationAfterCommit_defersEventDispatchUntilCommit() {
        NotificationSseService service = new NotificationSseService(new NotificationSseProperties(true, 1L, 1L));
        service.subscribe(1L);

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.publishNotificationAfterCommit(1L, 100L);

            assertThat(notificationEventCount(service)).isZero();
            assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);

            triggerAfterCommit();

            assertThat(notificationEventCount(service)).isEqualTo(1L);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private void triggerAfterCommit() {
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }
    }

    private NotificationEmitterRegistry emitterRegistry(NotificationSseService service) {
        return (NotificationEmitterRegistry) fieldValue(service, "emitterRegistry");
    }

    private long notificationEventCount(NotificationSseService service) {
        return ((AtomicLong) fieldValue(service, "sseNotificationEvents")).get();
    }

    private Object fieldValue(Object target, String fieldName) {
        try {
            Field field = NotificationSseService.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("테스트 필드 접근에 실패했습니다: " + fieldName, e);
        }
    }
}
