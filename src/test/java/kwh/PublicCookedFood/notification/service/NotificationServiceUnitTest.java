package kwh.PublicCookedFood.notification.service;

import kwh.PublicCookedFood.notification.repository.NotificationRepository;
import kwh.PublicCookedFood.user.domain.Role;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.repository.UserRepository;
import kwh.PublicCookedFood.user.service.UserActivityLogService;
import kwh.PublicCookedFood.user.service.UserBlockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceUnitTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserBlockService userBlockService;

    @Mock
    private UserActivityLogService userActivityLogService;

    @InjectMocks
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notificationService, "sseEnabled", true);
        getEmitters().clear();
    }

    @Test
    void updateNotificationEnabled_disableClearsActiveEmitters() {
        Users user = createUser(1L, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        TrackingEmitter emitter = new TrackingEmitter();
        getEmitters().put(1L, new ConcurrentHashMap<>(Map.of("1_1", emitter)));

        boolean enabled = notificationService.updateNotificationEnabled(1L, false);

        assertThat(enabled).isFalse();
        assertThat(user.isNotificationEnabled()).isFalse();
        assertThat(getEmitters()).doesNotContainKey(1L);
        assertThat(emitter.completed).isTrue();
    }

    @Test
    void subscribe_throwsWhenNotificationSettingDisabled() {
        Users user = createUser(2L, false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> notificationService.subscribe(2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("비활성화");
    }

    @Test
    void subscribe_registersEmitterWhenNotificationEnabled() {
        Users user = createUser(3L, true);
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));

        SseEmitter emitter = notificationService.subscribe(3L);

        assertThat(emitter).isNotNull();
        assertThat(getEmitters()).containsKey(3L);
        assertThat(getEmitters().get(3L)).isNotEmpty();
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Map<String, SseEmitter>> getEmitters() {
        return (Map<Long, Map<String, SseEmitter>>) ReflectionTestUtils.getField(notificationService, "emitters");
    }

    private Users createUser(Long id, boolean notificationEnabled) {
        return Users.builder()
                .id(id)
                .email("notification-" + id + "@test.com")
                .name("알림테스터")
                .notificationEnabled(notificationEnabled)
                .authority(Role.USER)
                .loginMethod("Current")
                .build();
    }

    private static final class TrackingEmitter extends SseEmitter {
        private boolean completed;

        @Override
        public synchronized void complete() {
            completed = true;
            super.complete();
        }
    }
}
