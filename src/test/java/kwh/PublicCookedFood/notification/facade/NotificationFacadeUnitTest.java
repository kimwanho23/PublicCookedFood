package kwh.PublicCookedFood.notification.facade;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.common.error.CommonErrorCode;
import kwh.PublicCookedFood.notification.dto.response.NotificationSettingResponse;
import kwh.PublicCookedFood.notification.error.NotificationErrorCode;
import kwh.PublicCookedFood.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationFacadeUnitTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationFacade notificationFacade;

    @Test
    void subscribe_throwsWhenNotificationDisabledInStoredAccountState() {
        Account account = loginAccount(7L, false);

        assertThatThrownBy(() -> notificationFacade.subscribe(account))
                .isInstanceOfSatisfying(AppException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(NotificationErrorCode.NOTIFICATION_DISABLED));

        verify(notificationService, never()).subscribe(7L);
    }

    @Test
    void getNotificationSetting_throwsWhenAccountIsMissing() {
        assertThatThrownBy(() -> notificationFacade.getNotificationSetting(null))
                .isInstanceOfSatisfying(AppException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(CommonErrorCode.AUTHENTICATION_REQUIRED));
    }

    @Test
    void subscribe_delegatesWhenNotificationEnabled() {
        Account account = loginAccount(8L, true);
        SseEmitter emitter = new SseEmitter();
        when(notificationService.subscribe(8L)).thenReturn(emitter);

        SseEmitter result = notificationFacade.subscribe(account);

        assertThat(result).isSameAs(emitter);
        verify(notificationService).subscribe(8L);
    }

    @Test
    void updateNotificationSetting_updatesCurrentSessionAccountSnapshot() {
        Account account = loginAccount(9L, true);
        when(notificationService.updateNotificationEnabled(9L, false)).thenReturn(false);

        NotificationSettingResponse response = notificationFacade.updateNotificationSetting(account, false);

        assertThat(response.enabled()).isFalse();
        assertThat(account.isNotificationEnabled()).isFalse();
        verify(notificationService).updateNotificationEnabled(9L, false);
    }

    private Account loginAccount(Long id, boolean notificationEnabled) {
        return Account.builder()
                .id(id)
                .email("notify-" + id + "@test.com")
                .name("notify-account")
                .authority(Role.USER)
                .loginMethod("Current")
                .notificationEnabled(notificationEnabled)
                .build();
    }
}
