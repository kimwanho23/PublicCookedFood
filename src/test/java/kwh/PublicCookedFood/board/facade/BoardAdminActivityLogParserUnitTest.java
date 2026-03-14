package kwh.PublicCookedFood.board.facade;

import kwh.PublicCookedFood.account.domain.AccountActivityLog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BoardAdminActivityLogParserUnitTest {

    private final BoardAdminActivityLogParser parser = new BoardAdminActivityLogParser();

    @Test
    void parse_extractsTypedIdsFromDetail() {
        AccountActivityLog activity = AccountActivityLog.builder()
                .action("BOARD_COMMENT_CREATE")
                .detail("boardId=10,commentId=30,parentId=-")
                .build();

        BoardAdminParsedActivity parsed = parser.parse(activity);

        assertThat(parsed.action()).isEqualTo(BoardAdminActivityAction.BOARD_COMMENT_CREATE);
        assertThat(parsed.detail().boardId()).contains(10L);
        assertThat(parsed.detail().commentId()).contains(30L);
        assertThat(parsed.detail().reportId()).isEmpty();
    }

    @Test
    void parse_ignoresInvalidValuesAndUnknownAction() {
        AccountActivityLog activity = AccountActivityLog.builder()
                .action("NOT_A_REAL_ACTION")
                .detail("boardId=-1,commentId=text,targetAccountId=7")
                .build();

        BoardAdminParsedActivity parsed = parser.parse(activity);

        assertThat(parsed.action()).isEqualTo(BoardAdminActivityAction.UNKNOWN);
        assertThat(parsed.detail().boardId()).isEmpty();
        assertThat(parsed.detail().commentId()).isEmpty();
        assertThat(parsed.detail().targetAccountId()).contains(7L);
    }

    @Test
    void parse_ignoresUnknownDetailKeysAndBlankTokens() {
        AccountActivityLog activity = AccountActivityLog.builder()
                .action("BOARD_REPORT_CREATE")
                .detail(" boardId = 8 , unknown = 99 ,, reportId=15 ")
                .build();

        BoardAdminParsedActivity parsed = parser.parse(activity);

        assertThat(parsed.action()).isEqualTo(BoardAdminActivityAction.BOARD_REPORT_CREATE);
        assertThat(parsed.detail().boardId()).contains(8L);
        assertThat(parsed.detail().reportId()).contains(15L);
        assertThat(parsed.detail().commentId()).isEmpty();
    }
}
