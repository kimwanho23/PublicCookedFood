package kwh.PublicCookedFood.account.controller;

import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.facade.BookmarkFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookmarkControllerUnitTest {

    @Mock
    private BookmarkFacade bookmarkFacade;

    @InjectMocks
    private BookmarkController bookmarkController;

    @Test
    void addBookmark_marksSkipViewIncreaseWhenRedirectingToDetail() {
        Account account = loginAccount(1L);
        when(bookmarkFacade.addBookmark(1L, 10L)).thenReturn(true);
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String viewName = bookmarkController.addBookmark(account, 10L, null, redirectAttributes);

        assertThat(viewName).isEqualTo("redirect:/recipes/10");
        assertThat(redirectAttributes.getFlashAttributes().get("skipViewIncrease"))
                .isEqualTo(Boolean.TRUE);
    }

    @Test
    void deleteBookmark_marksSkipViewIncreaseWhenRedirectingToDetail() {
        Account account = loginAccount(1L);
        when(bookmarkFacade.removeBookmark(account, 10L)).thenReturn(true);
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String viewName = bookmarkController.deleteBookmark(account, 10L, null, redirectAttributes);

        assertThat(viewName).isEqualTo("redirect:/recipes/10");
        assertThat(redirectAttributes.getFlashAttributes().get("skipViewIncrease"))
                .isEqualTo(Boolean.TRUE);
    }

    @Test
    void addBookmark_doesNotMarkSkipWhenRequestFails() {
        Account account = loginAccount(1L);
        when(bookmarkFacade.addBookmark(1L, 10L)).thenReturn(false);
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String viewName = bookmarkController.addBookmark(account, 10L, null, redirectAttributes);

        assertThat(viewName).isEqualTo("redirect:/recipes");
        assertThat(redirectAttributes.getFlashAttributes()).doesNotContainKey("skipViewIncrease");
    }

    @Test
    void deleteBookmark_redirectsToSafePathWhenProvided() {
        Account account = loginAccount(1L);
        when(bookmarkFacade.removeBookmark(account, 10L)).thenReturn(true);
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String viewName = bookmarkController.deleteBookmark(account, 10L, "/bookmarks", redirectAttributes);

        assertThat(viewName).isEqualTo("redirect:/bookmarks");
        assertThat(redirectAttributes.getFlashAttributes()).doesNotContainKey("skipViewIncrease");
    }

    private Account loginAccount(Long accountId) {
        return Account.builder()
                .id(accountId)
                .email("account@test.com")
                .name("tester")
                .authority(Role.USER)
                .loginMethod("Current")
                .build();
    }
}

