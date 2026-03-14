package kwh.PublicCookedFood.board.service.support;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

@Component
public class BoardContentSanitizer {

    private static final Safelist BOARD_CONTENT_SAFELIST = Safelist.relaxed()
            .addTags("iframe")
            .addAttributes("iframe", "src", "title", "width", "height", "frameborder", "allow", "allowfullscreen")
            .addProtocols("img", "src", "http", "https")
            .addProtocols("iframe", "src", "http", "https")
            .preserveRelativeLinks(true);

    public SanitizedBoardContent sanitize(String rawTitle, String rawContents) {
        return new SanitizedBoardContent(
                sanitizeTitle(rawTitle),
                sanitizeContents(rawContents)
        );
    }

    public String sanitizeTitle(String rawTitle) {
        return Jsoup.clean(rawTitle == null ? "" : rawTitle, Safelist.none());
    }

    public String sanitizeContents(String rawContents) {
        return Jsoup.clean(rawContents == null ? "" : rawContents, BOARD_CONTENT_SAFELIST);
    }
}
