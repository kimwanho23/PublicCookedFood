package kwh.PublicCookedFood.notification.service.dispatch;

import kwh.PublicCookedFood.account.domain.Account;

import java.util.Set;

final class RestrictedReceivers {

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
