package kwh.PublicCookedFood.notification.service;

import kwh.PublicCookedFood.notification.repository.NotificationRepository;
import kwh.PublicCookedFood.notification.service.dispatch.NotificationDispatchFacade;
import kwh.PublicCookedFood.user.audit.NotificationAuditPublisher;
import kwh.PublicCookedFood.user.domain.Role;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceUnitTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationDispatchFacade notificationDispatchFacade;

    @Mock
    private NotificationAuditPublisher notificationAuditPublisher;

    @Mock
    private NotificationSseService notificationSseService;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void updateNotificationEnabled_disableClearsActiveEmitters() {
        Users user = createUser(1L, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        boolean enabled = notificationService.updateNotificationEnabled(1L, false);

        assertThat(enabled).isFalse();
        assertThat(user.isNotificationEnabled()).isFalse();
        verify(notificationAuditPublisher).notificationSettingUpdate(1L, false);
        verify(notificationSseService).clearEmitters(1L);
    }

    @Test
    void subscribe_throwsWhenNotificationSettingDisabled() {
        Users user = createUser(2L, false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(notificationSseService.isSseEnabled()).thenReturn(true);

        assertThatThrownBy(() -> notificationService.subscribe(2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("비활성화");
    }

    @Test
    void subscribe_registersEmitterWhenNotificationEnabled() {
        Users user = createUser(3L, true);
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));
        when(notificationSseService.isSseEnabled()).thenReturn(true);
        SseEmitter expected = new SseEmitter();
        when(notificationSseService.subscribe(3L)).thenReturn(expected);

        SseEmitter emitter = notificationService.subscribe(3L);

        assertThat(emitter).isSameAs(expected);
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

}
