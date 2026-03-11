package kwh.PublicCookedFood.notification.service.dispatch;

import kwh.PublicCookedFood.notification.domain.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NewCommentNotificationDispatchStrategy {

    private final CommentNotificationContextFactory contextFactory;
    private final CommentNotificationPlanner planner;
    private final NotificationPublisher notificationPublisher;

    public void dispatch(NewCommentDispatchCommand command) {
        CommentNotificationContext context = contextFactory.create(command);
        CommentNotificationPlan plan = planner.plan(context);
        List<Notification> notifications = plan.toNotifications(context);
        notificationPublisher.publishAll(notifications);
    }
}
