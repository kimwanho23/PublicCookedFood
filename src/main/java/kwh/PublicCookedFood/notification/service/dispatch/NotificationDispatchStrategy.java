package kwh.PublicCookedFood.notification.service.dispatch;

public interface NotificationDispatchStrategy {

    NotificationDispatchType type();

    void dispatch(NotificationDispatchContext context);
}

