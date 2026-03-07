package kwh.PublicCookedFood.metrics.view;

public final class ViewCounterRedisKeys {

    public static final String BOARD_TOTAL_KEY_PREFIX = "view:board:total:";
    public static final String BOARD_DELTA_KEY_PREFIX = "view:board:delta:";
    public static final String BOARD_DIRTY_SET_KEY = "view:board:dirty";

    private ViewCounterRedisKeys() {
    }

    public static String boardTotalKey(Long boardId) {
        return BOARD_TOTAL_KEY_PREFIX + boardId;
    }

    public static String boardDeltaKey(Long boardId) {
        return BOARD_DELTA_KEY_PREFIX + boardId;
    }
}
