package kwh.PublicCookedFood.config;

import jakarta.servlet.http.HttpServletRequest;
import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.common.error.CommonErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalPageExceptionHandlerUnitTest {

    private final GlobalPageExceptionHandler globalPageExceptionHandler = new GlobalPageExceptionHandler();

    @Test
    void handleAppException_addsErrorCodeAndMessageToFlashAttributes() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/boards/10");

        String viewName = globalPageExceptionHandler.handleAppException(
                new AppException(CommonErrorCode.ACCESS_DENIED, "차단 관계인 사용자의 게시글은 조회할 수 없습니다."),
                request,
                redirectAttributes
        );

        assertThat(viewName).isEqualTo("redirect:/boards");
        assertThat(redirectAttributes.getFlashAttributes().get("globalErrorCode"))
                .isEqualTo(CommonErrorCode.ACCESS_DENIED.code());
        assertThat(redirectAttributes.getFlashAttributes().get("globalErrorMessage"))
                .isEqualTo("차단 관계인 사용자의 게시글은 조회할 수 없습니다.");
    }
}
