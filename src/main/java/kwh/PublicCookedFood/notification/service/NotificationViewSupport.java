package kwh.PublicCookedFood.notification.service;

import kwh.PublicCookedFood.account.service.AccountBlockService;
import kwh.PublicCookedFood.board.domain.SoftDeleteState;
import kwh.PublicCookedFood.common.util.ImmutableCollections;
import kwh.PublicCookedFood.notification.domain.Notification;
import kwh.PublicCookedFood.notification.domain.NotificationType;
import kwh.PublicCookedFood.notification.dto.response.NotificationResponse;
import kwh.PublicCookedFood.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class NotificationViewSupport {

    private final NotificationRepository notificationRepository;
    private final AccountBlockService accountBlockService;
    private final NotificationTargetPathResolver notificationTargetPathResolver;

    @Transactional(readOnly = true)
    public Page<NotificationResponse> loadVisibleNotificationPage(long receiverId,
                                                                  Pageable pageable,
                                                                  boolean unreadOnly) {
        NotificationVisibilityCriteria criteria = resolveCriteria(receiverId, unreadOnly);
        Page<Notification> notifications = loadVisibleNotifications(criteria, pageable);
        Map<Long, Map<Long, String>> commentTargetPaths =
                notificationTargetPathResolver.precomputePaths(notifications.getContent(), receiverId);
        return notifications.map(notification -> toResponse(notification, receiverId, commentTargetPaths));
    }

    @Transactional(readOnly = true)
    public long countVisibleUnreadNotifications(long receiverId) {
        return loadVisibleNotificationCount(resolveCriteria(receiverId, true));
    }

    private NotificationVisibilityCriteria resolveCriteria(long receiverId, boolean unreadOnly) {
        return NotificationVisibilityCriteria.of(
                receiverId,
                accountBlockService.getViewRestrictedAccountIds(receiverId),
                unreadOnly
        );
    }

    private NotificationResponse toResponse(Notification notification,
                                            long receiverId,
                                            Map<Long, Map<Long, String>> commentTargetPaths) {
        String targetPath = notificationTargetPathResolver.resolveTargetPath(notification, receiverId, commentTargetPaths);
        return NotificationResponse.from(notification, targetPath);
    }

    private Page<Notification> loadVisibleNotifications(NotificationVisibilityCriteria criteria,
                                                        Pageable pageable) {
        return notificationRepository.findVisibleNotificationsByCriteria(
                criteria.receiverId(),
                criteria.unreadOnly(),
                criteria.activeState(),
                criteria.reportResultTypes(),
                criteria.excludeRestricted(),
                criteria.restrictedAccountIdsOrSentinel(),
                pageable
        );
    }

    private long loadVisibleNotificationCount(NotificationVisibilityCriteria criteria) {
        return notificationRepository.countVisibleNotificationsByCriteria(
                criteria.receiverId(),
                criteria.unreadOnly(),
                criteria.activeState(),
                criteria.reportResultTypes(),
                criteria.excludeRestricted(),
                criteria.restrictedAccountIdsOrSentinel()
        );
    }

    static record NotificationVisibilityCriteria(long receiverId,
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
}
