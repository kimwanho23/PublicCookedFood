package kwh.PublicCookedFood.board.service.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BoardContentSanitizerUnitTest {

    private final BoardContentSanitizer boardContentSanitizer = new BoardContentSanitizer();

    @Test
    void sanitizeContents_stripsInlineStyles() {
        SanitizedBoardContent sanitized = boardContentSanitizer.sanitize(
                "<b>Title</b>",
                "<p><span style=\"font-size: 100px; font-family: fantasy; color: red;\">bad</span></p>"
        );

        assertThat(sanitized.title()).isEqualTo("Title");
        assertThat(sanitized.contents()).doesNotContain("style=");
    }
}
