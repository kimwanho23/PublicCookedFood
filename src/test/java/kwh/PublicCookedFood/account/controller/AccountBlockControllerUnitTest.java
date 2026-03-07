package kwh.PublicCookedFood.account.controller;

import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.facade.AccountBlockFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountBlockControllerUnitTest {

    @Mock
    private AccountBlockFacade accountBlockFacade;

    @InjectMocks
    private AccountBlockController accountBlockController;

    @Test
    void block_marksSkipViewIncreaseAndRedirectsBack() {
        Account account = loginAccount(1L);
        when(accountBlockFacade.block(1L, 2L))
                .thenReturn(AccountBlockFacade.BlockOperationResult.of("사용자를 차단했습니다."));
        MockHttpServletRequest request = requestWithReferer(
                "POST",
                "/u/blocks/2",
                "http://localhost/boards/10?page=2"
        );
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String viewName = accountBlockController.block(account, 2L, request, redirectAttributes);

        assertThat(viewName).isEqualTo("redirect:/boards/10?page=2");
        assertThat(redirectAttributes.getFlashAttributes().get("blockMessage"))
                .isEqualTo("사용자를 차단했습니다.");
        assertThat(redirectAttributes.getFlashAttributes().get("skipViewIncrease"))
                .isEqualTo(Boolean.TRUE);
    }

    @Test
    void unblock_marksSkipViewIncreaseEvenWhenRefererIsRejected() {
        Account account = loginAccount(1L);
        when(accountBlockFacade.unblock(1L, 2L))
                .thenReturn(AccountBlockFacade.BlockOperationResult.of("사용자 차단을 해제했습니다."));
        MockHttpServletRequest request = requestWithReferer(
                "PATCH",
                "/u/blocks/2",
                "http://evil.com/boards/10"
        );
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String viewName = accountBlockController.unblock(account, 2L, request, redirectAttributes);

        assertThat(viewName).isEqualTo("redirect:/boards");
        assertThat(redirectAttributes.getFlashAttributes().get("blockMessage"))
                .isEqualTo("사용자 차단을 해제했습니다.");
        assertThat(redirectAttributes.getFlashAttributes().get("skipViewIncrease"))
                .isEqualTo(Boolean.TRUE);
    }

    private MockHttpServletRequest requestWithReferer(String method,
                                                      String uri,
                                                      String referer) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setServerName("localhost");
        request.setScheme("http");
        request.setServerPort(80);
        request.addHeader("Referer", referer);
        return request;
    }

    private Account loginAccount(Long accountId) {
        return Account.builder()
                .id(accountId)
                .email("viewer@test.com")
                .name("viewer")
                .authority(Role.USER)
                .loginMethod("Current")
                .build();
    }
}

