package kwh.PublicCookedFood.notification.service.dispatch;

import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.StringJoiner;

@Component
public class NotificationPreviewFactory {

    private static final int MAX_PREVIEW_LENGTH = 255;

    public String buildPreview(String contents) {
        if (!StringUtils.hasText(contents)) {
            return "";
        }
        String normalized = contents.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= MAX_PREVIEW_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_PREVIEW_LENGTH);
    }

    public String extractPlainText(String htmlContents) {
        return Jsoup.parse(StringUtils.hasText(htmlContents) ? htmlContents : "").text();
    }

    public String buildPreviewFromParts(String... parts) {
        StringJoiner joiner = new StringJoiner(" ");
        for (String part : parts) {
            if (StringUtils.hasText(part)) {
                joiner.add(part.trim());
            }
        }
        return buildPreview(joiner.toString());
    }

    public String buildBoardPreview(String title, String htmlContents) {
        return buildPreviewFromParts(title, extractPlainText(htmlContents));
    }
}
