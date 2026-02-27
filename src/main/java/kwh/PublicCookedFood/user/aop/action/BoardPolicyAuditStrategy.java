package kwh.PublicCookedFood.user.aop.action;

import kwh.PublicCookedFood.user.aop.UserActionAuditType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Consumer;

@Component
@Slf4j
public class BoardPolicyAuditStrategy extends AbstractUserActionAuditStrategy {

    public BoardPolicyAuditStrategy(UserActionAuditRecorder recorder) {
        super(createHandlers(recorder));
    }

    private static Map<UserActionAuditType, Consumer<UserActionAuditArgs>> createHandlers(
            UserActionAuditRecorder recorder) {
        Map<UserActionAuditType, Consumer<UserActionAuditArgs>> handlers = newHandlers();

        handlers.put(UserActionAuditType.BOARD_POLICY_UPDATE, args -> {
            Long userId = args.asLong(0);
            Integer threshold = args.asInteger(1);
            String thumbnailDisplayMode = args.asString(2);
            log.info("action=board.policy_update result=success userId={} featuredLikeThreshold={} thumbnailDisplayMode={}",
                    userId, threshold, thumbnailDisplayMode);
            recorder.record(userId, "BOARD_POLICY_UPDATE",
                    "featuredLikeThreshold=" + args.safeNumber(threshold)
                            + ",thumbnailDisplayMode=" + args.safeText(thumbnailDisplayMode));
        });

        return handlers;
    }
}
