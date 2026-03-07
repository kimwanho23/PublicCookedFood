package kwh.PublicCookedFood.notification.service.dispatch;

import kwh.PublicCookedFood.board.domain.BoardReportStatus;
import kwh.PublicCookedFood.notification.domain.Notification;
import kwh.PublicCookedFood.notification.domain.NotificationType;
import kwh.PublicCookedFood.notification.repository.NotificationRepository;
import kwh.PublicCookedFood.notification.service.NotificationSseService;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.policy.AccountNicknamePolicy;
import kwh.PublicCookedFood.account.repository.AccountRepository;
import kwh.PublicCookedFood.account.service.AccountBlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class NotificationDispatchSupport {

    private static final int MAX_PREVIEW_LENGTH = 255;
    private static final int MAX_MENTION_USERS = 5;
    private static final Pattern MENTION_PATTERN = Pattern.compile("@(" + AccountNicknamePolicy.REGEX_BODY + ")");

    private final NotificationRepository notificationRepository;
    private final AccountRepository accountRepository;
    private final AccountBlockService accountBlockService;
    private final NotificationSseService notificationSseService;

    public Notification saveAndPublish(Notification notification) {
        Notification saved = notificationRepository.save(notification);
        if (saved.getReceiver() != null && saved.getReceiver().getId() != null && saved.getId() != null) {
            notificationSseService.publishNotificationAfterCommit(saved.getReceiver().getId(), saved.getId());
        }
        return saved;
    }

    public Set<Account> resolveMentionedUsers(String rawText, Long actorAccountId) {
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

        Set<Account> resolvedUsers = new LinkedHashSet<>();
        for (String mentionName : mentionNames) {
            Account candidate = resolveMentionedUser(mentionName);
            if (candidate == null) {
                continue;
            }
            if (candidate.getId() == null || Objects.equals(candidate.getId(), actorAccountId)) {
                continue;
            }
            resolvedUsers.add(candidate);
        }
        return resolvedUsers;
    }

    private Account resolveMentionedUser(String mentionName) {
        return accountRepository.findByName(mentionName).orElse(null);
    }

    public boolean canReceiveNotification(Account receiver, Account actor) {
        if (receiver == null || receiver.getId() == null) {
            return false;
        }
        if (!receiver.isNotificationEnabled()) {
            return false;
        }
        if (actor == null || actor.getId() == null) {
            return true;
        }
        return !accountBlockService.isEitherBlocked(receiver.getId(), actor.getId());
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

    public boolean isSameAccount(Account left, Account right) {
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

