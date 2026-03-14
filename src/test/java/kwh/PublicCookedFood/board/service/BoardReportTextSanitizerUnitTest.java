package kwh.PublicCookedFood.board.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BoardReportTextSanitizerUnitTest {

    private final BoardReportTextSanitizer sanitizer = new BoardReportTextSanitizer();

    @Test
    void sanitizeDetails_returnsEmptyWhenBlankAfterSanitizing() {
        assertThat(sanitizer.sanitizeDetails("   <b> </b>   ").isPresent()).isFalse();
    }

    @Test
    void sanitizeDetails_normalizesWhitespaceAndStripsHtml() {
        assertThat(sanitizer.sanitizeDetails("  <b>spam</b>\n  report  "))
                .contains("spam report");
    }

    @Test
    void sanitizeProcessNote_truncatesToConfiguredLength() {
        String longText = "a".repeat(600);

        assertThat(sanitizer.sanitizeProcessNote(longText)).hasValueSatisfying(value -> assertThat(value).hasSize(500));
    }
}
