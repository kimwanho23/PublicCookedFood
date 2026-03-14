package kwh.PublicCookedFood.config.oauth2;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.account.dto.CustomAccountDetails;
import kwh.PublicCookedFood.account.service.AccountService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.ServletWebRequest;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginAccountArgumentResolverUnitTest {

    @Mock
    private AccountService accountService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolveArgument_usesPrincipalSnapshotForNotificationStream() throws Exception {
        LoginAccountArgumentResolver resolver = new LoginAccountArgumentResolver(accountService);
        Account principalAccount = account(1L, "stream-user");
        setAuthentication(principalAccount);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/notifications/stream");
        request.setServletPath("/api/notifications/stream");

        Object resolved = resolver.resolveArgument(
                loginAccountParameter(),
                null,
                new ServletWebRequest(request),
                null
        );

        assertThat(resolved).isSameAs(principalAccount);
        verifyNoInteractions(accountService);
    }

    @Test
    void resolveArgument_loadsFreshAccountForRegularRequestWhenRequestCacheMissing() throws Exception {
        LoginAccountArgumentResolver resolver = new LoginAccountArgumentResolver(accountService);
        Account principalAccount = account(1L, "principal-user");
        Account freshAccount = account(1L, "fresh-user");
        setAuthentication(principalAccount);
        when(accountService.findById(1L)).thenReturn(Optional.of(freshAccount));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/boards");
        request.setServletPath("/boards");

        Object resolved = resolver.resolveArgument(
                loginAccountParameter(),
                null,
                new ServletWebRequest(request),
                null
        );

        assertThat(resolved).isSameAs(freshAccount);
        verify(accountService).findById(1L);
    }

    private void setAuthentication(Account account) {
        CustomAccountDetails accountDetails = new CustomAccountDetails(account);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                accountDetails,
                null,
                accountDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private MethodParameter loginAccountParameter() throws NoSuchMethodException {
        Method method = TestController.class.getDeclaredMethod("handle", Account.class);
        return new MethodParameter(method, 0);
    }

    private Account account(Long accountId, String name) {
        return Account.builder()
                .id(accountId)
                .email(name + "@test.com")
                .name(name)
                .authority(Role.USER)
                .loginMethod("Current")
                .notificationEnabled(true)
                .build();
    }

    private static class TestController {

        @SuppressWarnings("unused")
        void handle(@LoginAccount Account account) {
        }
    }
}
