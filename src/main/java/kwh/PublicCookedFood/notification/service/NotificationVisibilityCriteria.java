package kwh.PublicCookedFood.notification.service;

import kwh.PublicCookedFood.board.domain.SoftDeleteState;
import kwh.PublicCookedFood.common.util.ImmutableCollections;
import kwh.PublicCookedFood.notification.domain.NotificationType;

import java.util.Collection;
import java.util.List;
import java.util.Set;

record NotificationVisibilityCriteria(long receiverId,
                                      boolean unreadOnly,
                                      Set<Long> restrictedAccountIds) {

    private static final long NO_RESTRICTED_ACCOUNT_SENTINEL = -1L;

    NotificationVisibilityCriteria {
        restrictedAccountIds = ImmutableCollections.immutableSet(restrictedAccountIds);
    }

    static NotificationVisibilityCriteria of(long receiverId,
                                             Set<Long> restrictedAccountIds,
                                             boolean unreadOnly) {
        return new NotificationVisibilityCriteria(
                receiverId,
                unreadOnly,
                restrictedAccountIds
        );
    }

    SoftDeleteState activeState() {
        return SoftDeleteState.ACTIVE;
    }

    Collection<NotificationType> reportResultTypes() {
        return NotificationType.reportResultTypes();
    }

    boolean excludeRestricted() {
        return !restrictedAccountIds.isEmpty();
    }

    List<Long> restrictedAccountIdsOrSentinel() {
        if (restrictedAccountIds.isEmpty()) {
            return List.of(NO_RESTRICTED_ACCOUNT_SENTINEL);
        }
        return List.copyOf(restrictedAccountIds);
    }
}
