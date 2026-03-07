package kwh.PublicCookedFood.notification.repository;

import kwh.PublicCookedFood.board.domain.SoftDeleteState;
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
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByReceiverIdOrderByRegTimeDesc(Long receiverId, Pageable pageable);

    Page<Notification> findByReceiverIdAndIsReadFalseOrderByRegTimeDesc(Long receiverId, Pageable pageable);

    long countByReceiverIdAndIsReadFalse(Long receiverId);

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
    Page<Notification> findVisibleNotificationsByReceiverId(@Param("receiverId") Long receiverId,
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
            "AND n.isRead = false " +
            "AND b.state = :activeState " +
            "AND (b.hiddenByReport = false OR n.type IN :reportResultTypes) " +
            "AND (:excludeRestricted = false OR (n.actor.id NOT IN :restrictedAccountIds AND owner.id NOT IN :restrictedAccountIds))")
    long countVisibleUnreadNotificationsByReceiverId(@Param("receiverId") Long receiverId,
                                                     @Param("activeState") SoftDeleteState activeState,
                                                     @Param("reportResultTypes") Collection<NotificationType> reportResultTypes,
                                                     @Param("excludeRestricted") boolean excludeRestricted,
                                                     @Param("restrictedAccountIds") Collection<Long> restrictedAccountIds);

    @Query("SELECT n FROM Notification n " +
            "JOIN FETCH n.receiver " +
            "JOIN FETCH n.actor " +
            "JOIN FETCH n.board b " +
            "JOIN FETCH b.account " +
            "LEFT JOIN FETCH n.comment " +
            "WHERE n.receiver.id = :receiverId " +
            "ORDER BY n.regTime DESC")
    List<Notification> findAllWithActorBoardAndCommentByReceiverIdOrderByRegTimeDesc(@Param("receiverId") Long receiverId);

    @Query("SELECT n FROM Notification n " +
            "JOIN FETCH n.receiver " +
            "JOIN FETCH n.actor " +
            "JOIN FETCH n.board b " +
            "JOIN FETCH b.account " +
            "LEFT JOIN FETCH n.comment " +
            "WHERE n.receiver.id = :receiverId AND n.isRead = false " +
            "ORDER BY n.regTime DESC")
    List<Notification> findUnreadWithActorBoardAndCommentByReceiverIdOrderByRegTimeDesc(@Param("receiverId") Long receiverId);

    @Query("SELECT n FROM Notification n " +
            "JOIN FETCH n.receiver " +
            "JOIN FETCH n.actor " +
            "JOIN FETCH n.board b " +
            "JOIN FETCH b.account " +
            "LEFT JOIN FETCH n.comment " +
            "WHERE n.id = :notificationId")
    Optional<Notification> findWithActorBoardAndCommentById(@Param("notificationId") Long notificationId);

    long deleteByIdAndReceiverId(Long id, Long receiverId);

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
