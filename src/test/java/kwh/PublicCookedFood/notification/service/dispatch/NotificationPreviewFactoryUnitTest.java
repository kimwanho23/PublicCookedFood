package kwh.PublicCookedFood.notification.service.dispatch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationPreviewFactoryUnitTest {

    private NotificationPreviewFactory notificationPreviewFactory;

    @BeforeEach
    void setUp() {
        notificationPreviewFactory = new NotificationPreviewFactory();
    }

    @Test
    void buildPreviewFromParts_joinsOnlyNonBlankParts() {
        String preview = notificationPreviewFactory.buildPreviewFromParts("  title  ", "", " body text ", null);

        assertThat(preview).isEqualTo("title body text");
    }

    @Test
    void buildBoardPreview_combinesTitleAndHtmlContents() {
        String preview = notificationPreviewFactory.buildBoardPreview(" title ", "<p>body <b>text</b></p>");

        assertThat(preview).isEqualTo("title body text");
    }

    @Test
    void extractPlainText_returnsEmptyTextForNullHtml() {
        assertThat(notificationPreviewFactory.extractPlainText(null)).isEmpty();
    }
}
