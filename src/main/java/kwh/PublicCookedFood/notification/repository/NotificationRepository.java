package kwh.PublicCookedFood.notification.repository;

import kwh.PublicCookedFood.notification.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByReceiverIdOrderByRegTimeDesc(Long receiverId, Pageable pageable);

    Page<Notification> findByReceiverIdAndIsReadFalseOrderByRegTimeDesc(Long receiverId, Pageable pageable);

    long countByReceiverIdAndIsReadFalse(Long receiverId);

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
