package kwh.PublicCookedFood.notification.service;

import jakarta.transaction.Transactional;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardReport;
import kwh.PublicCookedFood.board.domain.BoardReportStatus;
import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.notification.domain.Notification;
import kwh.PublicCookedFood.notification.domain.NotificationType;
import kwh.PublicCookedFood.notification.dto.response.NotificationResponse;
import kwh.PublicCookedFood.notification.repository.NotificationRepository;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.repository.UserRepository;
import kwh.PublicCookedFood.user.service.UserActivityLogService;
import kwh.PublicCookedFood.user.service.UserBlockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private static final int MAX_PREVIEW_LENGTH = 255;
    private static final int MAX_MENTION_USERS = 5;
    private static final long SSE_TIMEOUT_MS = 60L * 60L * 1000L;
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([\\p{L}\\p{N}_-]{2,30})");

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final UserBlockService userBlockService;
    private final UserActivityLogService userActivityLogService;
    private final Map<Long, Map<String, SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final AtomicLong sseSendAttempts = new AtomicLong(0);
    private final AtomicLong sseSendSuccess = new AtomicLong(0);
    private final AtomicLong sseSendFailure = new AtomicLong(0);
    private final AtomicLong sseNotificationEvents = new AtomicLong(0);
    private final AtomicLong sseHeartbeatEvents = new AtomicLong(0);
    private final AtomicLong lastLoggedAttempts = new AtomicLong(0);
    private final AtomicLong lastLoggedFailures = new AtomicLong(0);

    @Value("${app.notification.sse.enabled:true}")
    private boolean sseEnabled;

    public SseEmitter subscribe(Long receiverId) {
        if (!sseEnabled) {
            throw new IllegalStateException("알림 SSE가 비활성화되어 있습니다.");
        }
        if (!isNotificationEnabled(receiverId)) {
            throw new IllegalStateException("알림 수신이 비활성화되어 있습니다.");
        }
        String emitterId = receiverId + "_" + System.currentTimeMillis();
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitters.computeIfAbsent(receiverId, key -> new ConcurrentHashMap<>()).put(emitterId, emitter);
        emitter.onCompletion(() -> removeEmitter(receiverId, emitterId));
        emitter.onTimeout(() -> removeEmitter(receiverId, emitterId));
        emitter.onError(error -> removeEmitter(receiverId, emitterId));
        sendConnectedEvent(receiverId, emitterId, emitter);
        return emitter;
    }

    @Transactional
    public void notifyOnNewComment(Comments comment) {
        if (comment == null || comment.getBoard() == null || comment.getUser() == null) {
            return;
        }

        Users actor = comment.getUser();
        Board board = comment.getBoard();
        Users boardOwner = board.getUser();
        Comments parent = comment.getParent();
        Users parentCommentOwner = parent == null ? null : parent.getUser();
        String preview = buildPreview(comment.getContents());
        Set<Long> notifiedReceiverIds = new LinkedHashSet<>();

        if (!isSameUser(parentCommentOwner, actor)) {
            createReplyNotificationIfNeeded(parentCommentOwner, actor, board, comment, preview, notifiedReceiverIds);
        }

        if (boardOwner != null
                && !isSameUser(boardOwner, actor)
                && !isSameUser(boardOwner, parentCommentOwner)
                && canReceiveNotification(boardOwner, actor)) {
            Notification saved = notificationRepository.save(
                    Notification.boardComment(boardOwner, actor, board, comment, preview)
            );
            publishNotificationAfterCommit(boardOwner.getId(), saved.getId());
            notifiedReceiverIds.add(boardOwner.getId());
        }

        for (Users mentionedUser : resolveMentionedUsers(comment.getContents(), actor.getId())) {
            if (mentionedUser.getId() == null || notifiedReceiverIds.contains(mentionedUser.getId())) {
                continue;
            }
            if (!canReceiveNotification(mentionedUser, actor)) {
                continue;
            }
            Notification saved = notificationRepository.save(
                    Notification.commentMention(mentionedUser, actor, board, comment, preview)
            );
            publishNotificationAfterCommit(mentionedUser.getId(), saved.getId());
            notifiedReceiverIds.add(mentionedUser.getId());
        }
    }

    @Transactional
    public void notifyOnBoardCreated(Board board) {
        if (board == null || board.getUser() == null || board.getUser().getId() == null) {
            return;
        }

        Users actor = board.getUser();
        String plainText = Jsoup.parse(board.getContents() == null ? "" : board.getContents()).text();
        String preview = buildPreview(board.getTitle() == null ? plainText : board.getTitle() + " " + plainText);
        Set<Long> notifiedReceiverIds = new LinkedHashSet<>();

        for (Users mentionedUser : resolveMentionedUsers(plainText, actor.getId())) {
            if (mentionedUser.getId() == null || notifiedReceiverIds.contains(mentionedUser.getId())) {
                continue;
            }
            if (!canReceiveNotification(mentionedUser, actor)) {
                continue;
            }
            Notification saved = notificationRepository.save(
                    Notification.boardMention(mentionedUser, actor, board, preview)
            );
            publishNotificationAfterCommit(mentionedUser.getId(), saved.getId());
            notifiedReceiverIds.add(mentionedUser.getId());
        }
    }

    @Transactional
    public void notifyOnReportProcessed(BoardReport report) {
        if (report == null
                || report.getReporter() == null
                || report.getProcessor() == null
                || report.getBoard() == null
                || report.getReporter().getId() == null
                || report.getProcessor().getId() == null) {
            return;
        }
        if (report.getReporter().getId().equals(report.getProcessor().getId())) {
            return;
        }

        NotificationType notificationType = resolveReportNotificationType(report.getStatus());
        if (notificationType == null) {
            return;
        }
        if (!canReceiveNotification(report.getReporter(), report.getProcessor())) {
            return;
        }

        String previewSeed = report.getProcessedNote();
        if (previewSeed == null || previewSeed.isBlank()) {
            previewSeed = report.getReason() == null ? "신고 처리 결과가 등록되었습니다." : report.getReason().getLabel();
        }
        String preview = buildPreview(previewSeed);
        Notification saved = notificationRepository.save(Notification.reportResult(
                report.getReporter(),
                report.getProcessor(),
                report.getBoard(),
                notificationType,
                preview
        ));
        publishNotificationAfterCommit(report.getReporter().getId(), saved.getId());
    }

    @Transactional
    public void markAsRead(Long receiverId, Long notificationId) {
        notificationRepository.markAsRead(notificationId, receiverId, LocalDateTime.now());
    }

    @Transactional
    public int markAllAsRead(Long receiverId) {
        return notificationRepository.markAllAsRead(receiverId, LocalDateTime.now());
    }

    @Transactional
    public Page<NotificationResponse> getNotifications(Long receiverId, Pageable pageable, boolean unreadOnly) {
        if (unreadOnly) {
            return notificationRepository.findByReceiverIdAndIsReadFalseOrderByRegTimeDesc(receiverId, pageable)
                    .map(NotificationResponse::from);
        }
        return notificationRepository.findByReceiverIdOrderByRegTimeDesc(receiverId, pageable)
                .map(NotificationResponse::from);
    }

    @Transactional
    public long getUnreadCount(Long receiverId) {
        return notificationRepository.countByReceiverIdAndIsReadFalse(receiverId);
    }

    @Transactional
    public void deleteNotification(Long receiverId, Long notificationId) {
        notificationRepository.deleteByIdAndReceiverId(notificationId, receiverId);
    }

    @Transactional
    public long deleteAllNotifications(Long receiverId, boolean unreadOnly) {
        if (unreadOnly) {
            return notificationRepository.deleteByReceiverIdAndIsReadFalse(receiverId);
        }
        return notificationRepository.deleteByReceiverId(receiverId);
    }

    @Transactional
    public boolean updateNotificationEnabled(Long userId, boolean enabled) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자 정보를 찾을 수 없습니다."));
        user.updateNotificationEnabled(enabled);
        log.info("action=notification.setting_update result=success userId={} enabled={}", userId, enabled);
        userActivityLogService.record(
                userId,
                "NOTIFICATION_SETTING_UPDATE",
                "enabled=" + enabled
        );
        if (!enabled) {
            clearEmitters(userId);
        }
        return user.isNotificationEnabled();
    }

    @Transactional
    public boolean isNotificationEnabled(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자 정보를 찾을 수 없습니다."));
        return user.isNotificationEnabled();
    }

    private void createReplyNotificationIfNeeded(Users receiver,
                                                 Users actor,
                                                 Board board,
                                                 Comments comment,
                                                 String preview,
                                                 Set<Long> notifiedReceiverIds) {
        if (receiver == null || !canReceiveNotification(receiver, actor)) {
            return;
        }
        Notification saved = notificationRepository.save(Notification.commentReply(receiver, actor, board, comment, preview));
        publishNotificationAfterCommit(receiver.getId(), saved.getId());
        if (notifiedReceiverIds != null && receiver.getId() != null) {
            notifiedReceiverIds.add(receiver.getId());
        }
    }

    private Set<Users> resolveMentionedUsers(String rawText, Long actorUserId) {
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

    private NotificationType resolveReportNotificationType(BoardReportStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case RESOLVED -> NotificationType.REPORT_RESOLVED;
            case REJECTED -> NotificationType.REPORT_REJECTED;
            default -> null;
        };
    }

    private boolean canReceiveNotification(Users receiver, Users actor) {
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

    private String buildPreview(String contents) {
        if (contents == null) {
            return "";
        }
        String normalized = contents.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= MAX_PREVIEW_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_PREVIEW_LENGTH);
    }

    private boolean isSameUser(Users left, Users right) {
        if (left == null || right == null || left.getId() == null || right.getId() == null) {
            return false;
        }
        return left.getId().equals(right.getId());
    }

    private void publishNotificationAfterCommit(Long receiverId, Long notificationId) {
        Runnable publishTask = () -> sendNotificationEvent(receiverId, notificationId);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishTask.run();
                }
            });
            return;
        }
        publishTask.run();
    }

    private void sendConnectedEvent(Long receiverId, String emitterId, SseEmitter emitter) {
        sendToEmitter(receiverId, emitterId, emitter, "connected",
                Map.of("timestamp", System.currentTimeMillis()));
    }

    private void sendNotificationEvent(Long receiverId, Long notificationId) {
        Map<String, SseEmitter> userEmitters = emitters.get(receiverId);
        if (userEmitters == null || userEmitters.isEmpty()) {
            return;
        }
        sseNotificationEvents.incrementAndGet();
        long unreadCount = notificationRepository.countByReceiverIdAndIsReadFalse(receiverId);
        NotificationResponse latest = notificationRepository.findById(notificationId)
                .map(NotificationResponse::from)
                .orElse(null);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("unreadCount", unreadCount);
        payload.put("latest", latest);
        payload.put("timestamp", System.currentTimeMillis());

        for (Map.Entry<String, SseEmitter> entry : userEmitters.entrySet()) {
            sendToEmitter(receiverId, entry.getKey(), entry.getValue(), "notification", payload);
        }
    }

    @Scheduled(fixedDelayString = "${app.notification.sse.heartbeat-interval-ms:25000}")
    public void sendHeartbeat() {
        if (!sseEnabled || emitters.isEmpty()) {
            return;
        }
        sseHeartbeatEvents.incrementAndGet();
        Map<String, Object> payload = Map.of("timestamp", System.currentTimeMillis());
        for (Map.Entry<Long, Map<String, SseEmitter>> userEntry : emitters.entrySet()) {
            Long receiverId = userEntry.getKey();
            for (Map.Entry<String, SseEmitter> emitterEntry : userEntry.getValue().entrySet()) {
                sendToEmitter(receiverId, emitterEntry.getKey(), emitterEntry.getValue(), "heartbeat", payload);
            }
        }
    }

    @Scheduled(fixedDelayString = "${app.notification.sse.metrics-log-interval-ms:60000}")
    public void logSseMetrics() {
        if (!sseEnabled) {
            return;
        }
        long attempts = sseSendAttempts.get();
        long failures = sseSendFailure.get();
        long deltaAttempts = attempts - lastLoggedAttempts.getAndSet(attempts);
        long deltaFailures = failures - lastLoggedFailures.getAndSet(failures);
        double totalFailureRate = calculateFailureRate(failures, attempts);
        double intervalFailureRate = calculateFailureRate(deltaFailures, deltaAttempts);

        log.info("action=notification.sse_metrics result=snapshot activeConnections={} receivers={} attempts={} failures={} failureRate={}%, " +
                        "intervalAttempts={} intervalFailures={} intervalFailureRate={}% notificationEvents={} heartbeatEvents={}",
                getActiveEmitterCount(),
                emitters.size(),
                attempts,
                failures,
                formatRate(totalFailureRate),
                deltaAttempts,
                deltaFailures,
                formatRate(intervalFailureRate),
                sseNotificationEvents.get(),
                sseHeartbeatEvents.get());
    }

    private void sendToEmitter(Long receiverId,
                               String emitterId,
                               SseEmitter emitter,
                               String eventName,
                               Object data) {
        sseSendAttempts.incrementAndGet();
        try {
            emitter.send(SseEmitter.event()
                    .id(emitterId + ":" + System.currentTimeMillis())
                    .name(eventName)
                    .data(data));
            sseSendSuccess.incrementAndGet();
        } catch (IOException | IllegalStateException e) {
            sseSendFailure.incrementAndGet();
            removeEmitter(receiverId, emitterId);
            log.debug("Failed to send SSE event. receiverId={}, emitterId={}, eventName={}", receiverId, emitterId, eventName, e);
        }
    }

    private void removeEmitter(Long receiverId, String emitterId) {
        Map<String, SseEmitter> userEmitters = emitters.get(receiverId);
        if (userEmitters == null) {
            return;
        }
        userEmitters.remove(emitterId);
        if (userEmitters.isEmpty()) {
            emitters.remove(receiverId);
        }
    }

    private void clearEmitters(Long receiverId) {
        Map<String, SseEmitter> userEmitters = emitters.remove(receiverId);
        if (userEmitters == null) {
            return;
        }
        userEmitters.values().forEach(emitter -> {
            try {
                emitter.complete();
            } catch (RuntimeException ignored) {
            }
        });
    }

    private int getActiveEmitterCount() {
        return emitters.values().stream()
                .filter(Objects::nonNull)
                .mapToInt(Map::size)
                .sum();
    }

    private double calculateFailureRate(long failures, long attempts) {
        if (attempts <= 0) {
            return 0.0d;
        }
        return (failures * 100.0d) / attempts;
    }

    private String formatRate(double rate) {
        return String.format("%.2f", rate);
    }
}
