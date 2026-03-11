package kwh.PublicCookedFood.notification.service.dispatch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MentionNameExtractorUnitTest {

    private MentionNameExtractor mentionNameExtractor;

    @BeforeEach
    void setUp() {
        mentionNameExtractor = new MentionNameExtractor();
    }

    @Test
    void extract_deduplicatesMentionsAndKeepsEncounterOrder() {
        List<String> mentionNames = mentionNameExtractor.extract("@alpha @beta @alpha @gamma");

        assertThat(mentionNames).containsExactly("alpha", "beta", "gamma");
    }

    @Test
    void extract_limitsMentionCount() {
        List<String> mentionNames = mentionNameExtractor.extract("@aa @bb @cc @dd @ee @ff");

        assertThat(mentionNames).containsExactly("aa", "bb", "cc", "dd", "ee");
    }

    @Test
    void extract_ignoresEmailAndUrlShapedTokens() {
        List<String> mentionNames = mentionNameExtractor.extract(
                "문의는 foo@tester.com 또는 @tester.com 참고 https://example.com/@alpha/profile"
        );

        assertThat(mentionNames).isEmpty();
    }

    @Test
    void extract_ignoresOverlongMentionToken() {
        List<String> mentionNames = mentionNameExtractor.extract("@abcdefghijklmnopqrstu hello");

        assertThat(mentionNames).isEmpty();
    }

    @Test
    void extract_allowsMentionsWrappedByPunctuation() {
        List<String> mentionNames = mentionNameExtractor.extract("(@alpha), 안녕! @beta?");

        assertThat(mentionNames).containsExactly("alpha", "beta");
    }
}
