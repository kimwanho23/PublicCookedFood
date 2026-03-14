package kwh.PublicCookedFood.notification.repository;

import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import kwh.PublicCookedFood.notification.domain.Notification;
import kwh.PublicCookedFood.notification.domain.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query(
            value = "SELECT n FROM Notification n " +
                    "JOIN FETCH n.receiver " +
                    "JOIN FETCH n.actor " +
                    "JOIN FETCH n.board b " +
                    "JOIN FETCH b.account " +
                    "LEFT JOIN FETCH n.comment " +
                    "WHERE n.receiver.id = :receiverId " +
                    "AND (:unreadOnly = false OR n.isRead = false) " +
                    "AND b.state = :activeState " +
                    "AND (b.hiddenByReport = false OR n.type IN :reportResultTypes) " +
                    "AND (:excludeRestricted = false OR (n.actor.id NOT IN :restrictedAccountIds AND b.account.id NOT IN :restrictedAccountIds)) " +
                    "ORDER BY n.regTime DESC",
            countQuery = "SELECT COUNT(n) FROM Notification n " +
                    "JOIN n.board b " +
                    "JOIN b.account owner " +
                    "WHERE n.receiver.id = :receiverId " +
                    "AND (:unreadOnly = false OR n.isRead = false) " +
                    "AND b.state = :activeState " +
                    "AND (b.hiddenByReport = false OR n.type IN :reportResultTypes) " +
                    "AND (:excludeRestricted = false OR (n.actor.id NOT IN :restrictedAccountIds AND owner.id NOT IN :restrictedAccountIds))"
    )
    Page<Notification> findVisibleNotificationsByCriteria(@Param("receiverId") Long receiverId,
                                                          @Param("unreadOnly") boolean unreadOnly,
                                                          @Param("activeState") SoftDeleteState activeState,
                                                          @Param("reportResultTypes") Collection<NotificationType> reportResultTypes,
                                                          @Param("excludeRestricted") boolean excludeRestricted,
                                                          @Param("restrictedAccountIds") Collection<Long> restrictedAccountIds,
                                                          Pageable pageable);

    @Query("SELECT COUNT(n) FROM Notification n " +
            "JOIN n.board b " +
            "JOIN b.account owner " +
            "WHERE n.receiver.id = :receiverId " +
            "AND (:unreadOnly = false OR n.isRead = false) " +
            "AND b.state = :activeState " +
            "AND (b.hiddenByReport = false OR n.type IN :reportResultTypes) " +
            "AND (:excludeRestricted = false OR (n.actor.id NOT IN :restrictedAccountIds AND owner.id NOT IN :restrictedAccountIds))")
    long countVisibleNotificationsByCriteria(@Param("receiverId") Long receiverId,
                                             @Param("unreadOnly") boolean unreadOnly,
                                             @Param("activeState") SoftDeleteState activeState,
                                             @Param("reportResultTypes") Collection<NotificationType> reportResultTypes,
                                             @Param("excludeRestricted") boolean excludeRestricted,
                                             @Param("restrictedAccountIds") Collection<Long> restrictedAccountIds);

    long deleteByIdAndReceiverId(Long id, Long receiverId);

    boolean existsByIdAndReceiverId(Long id, Long receiverId);

    long deleteByReceiverId(Long receiverId);

    long deleteByReceiverIdAndIsReadFalse(Long receiverId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notification n " +
            "SET n.isRead = true, n.readTime = :readTime " +
            "WHERE n.id = :notificationId AND n.receiver.id = :receiverId AND n.isRead = false")
    int markAsRead(@Param("notificationId") Long notificationId,
                   @Param("receiverId") Long receiverId,
                   @Param("readTime") LocalDateTime readTime);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notification n " +
            "SET n.isRead = true, n.readTime = :readTime " +
            "WHERE n.receiver.id = :receiverId AND n.isRead = false")
    int markAllAsRead(@Param("receiverId") Long receiverId,
                      @Param("readTime") LocalDateTime readTime);
}
