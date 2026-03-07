package kwh.PublicCookedFood.common.error;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorMessageResolverUnitTest {

    @Test
    void resolve_returnsFallbackWhenCandidateIsBlank() {
        assertThat(ErrorMessageResolver.resolve("   ", "fallback")).isEqualTo("fallback");
    }

    @Test
    void resolve_returnsThrowableMessageWhenPresent() {
        assertThat(ErrorMessageResolver.resolve(new IllegalArgumentException("boom"), "fallback"))
                .isEqualTo("boom");
    }
}
