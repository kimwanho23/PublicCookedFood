package kwh.PublicCookedFood.notification.service.dispatch;

import kwh.PublicCookedFood.board.domain.BoardReportStatus;
import kwh.PublicCookedFood.notification.domain.Notification;
import kwh.PublicCookedFood.notification.domain.NotificationType;
import kwh.PublicCookedFood.notification.repository.NotificationRepository;
import kwh.PublicCookedFood.notification.service.NotificationSseService;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.repository.UserRepository;
import kwh.PublicCookedFood.user.service.UserBlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class NotificationDispatchSupport {

    private static final int MAX_PREVIEW_LENGTH = 255;
    private static final int MAX_MENTION_USERS = 5;
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([\\p{L}\\p{N}_-]{2,30})");

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final UserBlockService userBlockService;
    private final NotificationSseService notificationSseService;

    public Notification saveAndPublish(Notification notification) {
        Notification saved = notificationRepository.save(notification);
        if (saved.getReceiver() != null && saved.getReceiver().getId() != null && saved.getId() != null) {
            notificationSseService.publishNotificationAfterCommit(saved.getReceiver().getId(), saved.getId());
        }
        return saved;
    }

    public Set<Users> resolveMentionedUsers(String rawText, Long actorUserId) {
        if (rawText == null || rawText.isBlank()) {
            return Set.of();
        }

        Matcher matcher = MENTION_PATTERN.matcher(rawText);
        Set<String> mentionNames = new LinkedHashSet<>();
        while (matcher.find() && mentionNames.size() < MAX_MENTION_USERS) {
            String mentionName = matcher.group(1);
            if (mentionName != null && !mentionName.isBlank()) {
                mentionNames.add(mentionName.trim());
            }
        }
        if (mentionNames.isEmpty()) {
            return Set.of();
        }

        Set<Users> resolvedUsers = new LinkedHashSet<>();
        for (String mentionName : mentionNames) {
            List<Users> candidates = userRepository.findByName(mentionName);
            if (candidates.size() != 1) {
                continue;
            }
            Users candidate = candidates.get(0);
            if (candidate.getId() == null || Objects.equals(candidate.getId(), actorUserId)) {
                continue;
            }
            resolvedUsers.add(candidate);
        }
        return resolvedUsers;
    }

    public boolean canReceiveNotification(Users receiver, Users actor) {
        if (receiver == null || receiver.getId() == null) {
            return false;
        }
        if (!receiver.isNotificationEnabled()) {
            return false;
        }
        if (actor == null || actor.getId() == null) {
            return true;
        }
        return !userBlockService.isEitherBlocked(receiver.getId(), actor.getId());
    }

    public String buildPreview(String contents) {
        if (contents == null) {
            return "";
        }
        String normalized = contents.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= MAX_PREVIEW_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_PREVIEW_LENGTH);
    }

    public boolean isSameUser(Users left, Users right) {
        if (left == null || right == null || left.getId() == null || right.getId() == null) {
            return false;
        }
        return left.getId().equals(right.getId());
    }

    public NotificationType resolveReportNotificationType(BoardReportStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case RESOLVED -> NotificationType.REPORT_RESOLVED;
            case REJECTED -> NotificationType.REPORT_REJECTED;
            default -> null;
        };
    }
}

