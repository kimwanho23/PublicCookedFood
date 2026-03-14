package kwh.PublicCookedFood.common.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class SafeRedirectSupportUnitTest {

    @Test
    void normalizeRelativePath_keepsSafeRelativePathAndQuery() {
        assertThat(SafeRedirectSupport.normalizeRelativePath("/boards/10?page=2"))
                .contains("/boards/10?page=2");
    }

    @Test
    void normalizeRelativePath_rejectsAbsoluteUrl() {
        assertThat(SafeRedirectSupport.normalizeRelativePath("https://evil.example/boards/10"))
                .isEmpty();
    }

    @Test
    void resolveRefererRedirect_allowsSameOriginReferer() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/u/blocks/2");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(80);
        request.addHeader("Referer", "http://localhost/boards/10?page=2");

        assertThat(SafeRedirectSupport.resolveRefererRedirect(request, "/boards"))
                .isEqualTo("redirect:/boards/10?page=2");
    }

    @Test
    void resolveRefererRedirect_rejectsCrossOriginReferer() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/u/blocks/2");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(80);
        request.addHeader("Referer", "https://evil.example/boards/10");

        assertThat(SafeRedirectSupport.resolveRefererRedirect(request, "/boards"))
                .isEqualTo("redirect:/boards");
    }
}
