package kwh.PublicCookedFood.notification.service;

import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.notification.repository.NotificationRepository;
import kwh.PublicCookedFood.notification.error.NotificationErrorCode;
import kwh.PublicCookedFood.notification.service.dispatch.NotificationDispatchFacade;
import kwh.PublicCookedFood.account.audit.NotificationAuditPublisher;
import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    private AccountRepository accountRepository;

    @Mock
    private NotificationDispatchFacade notificationDispatchFacade;

    @Mock
    private NotificationAuditPublisher notificationAuditPublisher;

    @Mock
    private NotificationSseService notificationSseService;

    @Mock
    private NotificationViewSupport notificationViewSupport;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository,
                accountRepository,
                notificationAuditPublisher,
                notificationDispatchFacade,
                notificationSseService,
                notificationViewSupport
        );
    }

    @Test
    void updateNotificationEnabled_disableClearsActiveEmitters() {
        Account account = createAccount();
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        boolean enabled = notificationService.updateNotificationEnabled(1L, false);

        assertThat(enabled).isFalse();
        assertThat(account.isNotificationEnabled()).isFalse();
        verify(notificationAuditPublisher).notificationSettingUpdate(1L, false);
        verify(notificationSseService).clearEmitters(1L);
    }

    @Test
    void subscribe_throwsWhenNotificationSettingDisabled() {
        when(notificationSseService.isSseEnabled()).thenReturn(false);

        assertThatThrownBy(() -> notificationService.subscribe(2L))
                .isInstanceOfSatisfying(AppException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(NotificationErrorCode.NOTIFICATION_SSE_DISABLED);
                    assertThat(e.getMessage()).contains("비활성화");
                });
    }

    @Test
    void subscribe_registersEmitterWhenNotificationEnabled() {
        when(notificationSseService.isSseEnabled()).thenReturn(true);
        SseEmitter expected = new SseEmitter();
        when(notificationSseService.subscribe(3L)).thenReturn(expected);

        SseEmitter emitter = notificationService.subscribe(3L);

        assertThat(emitter).isSameAs(expected);
    }

    private Account createAccount() {
        return Account.builder()
                .id(1L)
                .email("notification-" + 1L + "@test.com")
                .name("알림테스터")
                .notificationEnabled(true)
                .authority(Role.USER)
                .loginMethod("Current")
                .build();
    }

}
