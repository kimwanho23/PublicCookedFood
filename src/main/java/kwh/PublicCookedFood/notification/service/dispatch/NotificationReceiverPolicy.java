package kwh.PublicCookedFood.notification.service.dispatch;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.service.AccountBlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class NotificationReceiverPolicy {

    private final AccountBlockService accountBlockService;

    boolean canReceiveFromActor(Account receiver, Account actor) {
        if (!receiver.isNotificationEnabled()) {
            return false;
        }
        return !accountBlockService.isEitherBlocked(receiver.getId(), actor.getId());
    }

    boolean canReceiveWithRestrictions(Account receiver, RestrictedReceivers restrictedReceivers) {
        if (!receiver.isNotificationEnabled()) {
            return false;
        }
        return restrictedReceivers.allows(receiver);
    }

    RestrictedReceivers resolveRestrictedReceivers(Account actor, Set<Account> candidateReceivers) {
        Long actorId = actor.getId();
        Set<Long> candidateIds = candidateReceivers.stream()
                .map(Account::getId)
                .collect(Collectors.toSet());
        if (candidateIds.isEmpty()) {
            return RestrictedReceivers.empty();
        }
        return RestrictedReceivers.of(accountBlockService.getRestrictedCounterpartyIds(actorId, candidateIds));
    }

    boolean isSameAccount(Account left, Account right) {
        return left.getId().equals(right.getId());
    }
}
