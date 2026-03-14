package kwh.PublicCookedFood.account.aop.action;

import kwh.PublicCookedFood.account.aop.AccountActionAuditType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Consumer;

@Component
@Slf4j
public class BoardPolicyAuditStrategy extends AbstractAccountActionAuditStrategy {

    public BoardPolicyAuditStrategy(AccountActionAuditRecorder recorder) {
        super(createHandlers(recorder));
    }

    private static Map<AccountActionAuditType, Consumer<AccountActionAuditArgs>> createHandlers(
            AccountActionAuditRecorder recorder) {
        Map<AccountActionAuditType, Consumer<AccountActionAuditArgs>> handlers = newHandlers();

        handlers.put(AccountActionAuditType.BOARD_POLICY_UPDATE, args -> {
            Long accountId = args.asLong(0);
            Integer threshold = args.asInteger(1);
            String thumbnailDisplayMode = args.asString(2);
            log.info("action=board.policy_update result=success accountId={} featuredLikeThreshold={} thumbnailDisplayMode={}",
                    args.displayId(accountId), args.displayNumber(threshold), args.displayText(thumbnailDisplayMode));
            recorder.record(accountId, "BOARD_POLICY_UPDATE",
                    "featuredLikeThreshold=" + args.displayNumber(threshold)
                            + ",thumbnailDisplayMode=" + args.displayText(thumbnailDisplayMode));
        });

        return handlers;
    }
}

