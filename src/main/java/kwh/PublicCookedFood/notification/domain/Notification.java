package kwh.PublicCookedFood.notification.domain;

import jakarta.persistence.*;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.common.BaseTimeEntity;
import kwh.PublicCookedFood.user.domain.Users;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notification", indexes = {
        @Index(name = "idx_notification_receiver_read_regtime", columnList = "receiver_id, is_read, regTime"),
        @Index(name = "idx_notification_receiver_regtime", columnList = "receiver_id, regTime"),
        @Index(name = "idx_notification_board_comment", columnList = "board_id, comment_id")
})
public class Notification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", referencedColumnName = "id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Users receiver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", referencedColumnName = "id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Users actor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", referencedColumnName = "id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Board board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", referencedColumnName = "id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Comments comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(name = "content_preview", length = 255)
    private String contentPreview;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "read_time")
    private LocalDateTime readTime;

    @Builder
    public Notification(Long id,
                        Users receiver,
                        Users actor,
                        Board board,
                        Comments comment,
                        NotificationType type,
                        String contentPreview,
                        boolean isRead,
                        LocalDateTime readTime) {
        this.id = id;
        this.receiver = receiver;
        this.actor = actor;
        this.board = board;
        this.comment = comment;
        this.type = type;
        this.contentPreview = contentPreview;
        this.isRead = isRead;
        this.readTime = readTime;
    }

    public static Notification boardComment(Users receiver,
                                            Users actor,
                                            Board board,
                                            Comments comment,
                                            String contentPreview) {
        return Notification.builder()
                .receiver(receiver)
                .actor(actor)
                .board(board)
                .comment(comment)
                .type(NotificationType.BOARD_COMMENT)
                .contentPreview(contentPreview)
                .isRead(false)
                .build();
    }

    public static Notification commentReply(Users receiver,
                                            Users actor,
                                            Board board,
                                            Comments comment,
                                            String contentPreview) {
        return Notification.builder()
                .receiver(receiver)
                .actor(actor)
                .board(board)
                .comment(comment)
                .type(NotificationType.COMMENT_REPLY)
                .contentPreview(contentPreview)
                .isRead(false)
                .build();
    }

    public static Notification boardMention(Users receiver,
                                            Users actor,
                                            Board board,
                                            String contentPreview) {
        return Notification.builder()
                .receiver(receiver)
                .actor(actor)
                .board(board)
                .comment(null)
                .type(NotificationType.BOARD_MENTION)
                .contentPreview(contentPreview)
                .isRead(false)
                .build();
    }

    public static Notification commentMention(Users receiver,
                                              Users actor,
                                              Board board,
                                              Comments comment,
                                              String contentPreview) {
        return Notification.builder()
                .receiver(receiver)
                .actor(actor)
                .board(board)
                .comment(comment)
                .type(NotificationType.COMMENT_MENTION)
                .contentPreview(contentPreview)
                .isRead(false)
                .build();
    }

    public static Notification reportResult(Users receiver,
                                            Users actor,
                                            Board board,
                                            NotificationType type,
                                            String contentPreview) {
        if (type != NotificationType.REPORT_RESOLVED && type != NotificationType.REPORT_REJECTED) {
            throw new IllegalArgumentException("신고 처리 알림 타입이 올바르지 않습니다.");
        }
        return Notification.builder()
                .receiver(receiver)
                .actor(actor)
                .board(board)
                .comment(null)
                .type(type)
                .contentPreview(contentPreview)
                .isRead(false)
                .build();
    }

    public void markAsRead(LocalDateTime readAt) {
        if (this.isRead) {
            return;
        }
        this.isRead = true;
        this.readTime = readAt == null ? LocalDateTime.now() : readAt;
    }
}
