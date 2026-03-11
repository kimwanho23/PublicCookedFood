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

    NotificationReceiverPolicy.RestrictedReceivers resolveRestrictedReceivers(Account actor, Set<Account> candidateReceivers) {
        Long actorId = actor.getId();
        Set<Long> candidateIds = candidateReceivers.stream()
                .map(Account::getId)
                .collect(Collectors.toSet());
        if (candidateIds.isEmpty()) {
            return RestrictedReceivers.empty();
        }
        return RestrictedReceivers.of(accountBlockService.getRestrictedCounterpartyIds(actorId, candidateIds));
    }

    static boolean isSameAccount(Account left, Account right) {
        return left != null
                && right != null
                && left.getId() != null
                && right.getId() != null
                && left.getId().equals(right.getId());
    }

    static final class RestrictedReceivers {

        private static final RestrictedReceivers EMPTY = new RestrictedReceivers(Set.of());

        private final Set<Long> receiverIds;

        private RestrictedReceivers(Set<Long> receiverIds) {
            this.receiverIds = receiverIds;
        }

        static RestrictedReceivers empty() {
            return EMPTY;
        }

        static RestrictedReceivers of(Set<Long> receiverIds) {
            if (receiverIds.isEmpty()) {
                return EMPTY;
            }
            return new RestrictedReceivers(receiverIds);
        }

        boolean allows(Account receiver) {
            return !receiverIds.contains(receiver.getId());
        }
    }
}
