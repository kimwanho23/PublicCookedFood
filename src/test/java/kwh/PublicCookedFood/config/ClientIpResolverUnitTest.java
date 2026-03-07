package kwh.PublicCookedFood.config;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientIpResolverUnitTest {

    private final ClientIpResolver clientIpResolver = new ClientIpResolver();

    @Test
    void resolve_returnsRemoteAddressWhenRequestIsNotFromTrustedProxy() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("203.0.113.7");

        assertThat(clientIpResolver.resolve(request)).isEqualTo("203.0.113.7");
    }

    @Test
    void resolve_prefersForwardedHeaderWhenRequestComesFromTrustedProxy() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("198.51.100.2, 127.0.0.1");

        assertThat(clientIpResolver.resolve(request)).isEqualTo("198.51.100.2");
    }

    @Test
    void resolve_returnsUnknownWhenRequestAndRemoteAddressAreMissing() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn(null);

        assertThat(clientIpResolver.resolve(request)).isEqualTo("unknown");
        assertThat(clientIpResolver.resolve(null)).isEqualTo("unknown");
    }
}
