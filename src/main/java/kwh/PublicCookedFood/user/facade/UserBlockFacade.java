package kwh.PublicCookedFood.user.facade;

import kwh.PublicCookedFood.user.audit.UserAuditPublisher;
import kwh.PublicCookedFood.user.service.UserBlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class UserBlockFacade {

    private final UserBlockService userBlockService;
    private final UserAuditPublisher userAuditPublisher;

    @Transactional(readOnly = true)
    public void auditBlockFailedUnauthenticated(Long targetUserId) {
        userAuditPublisher.userBlockFailedUnauthenticated(targetUserId);
    }

    @Transactional(readOnly = true)
    public void auditUnblockFailedUnauthenticated(Long targetUserId) {
        userAuditPublisher.userUnblockFailedUnauthenticated(targetUserId);
    }

    @Transactional
    public BlockOperationResult block(Long blockerId, Long targetUserId) {
        try {
            boolean created = userBlockService.block(blockerId, targetUserId);
            if (created) {
                userAuditPublisher.userBlockAdd(blockerId, targetUserId);
                return BlockOperationResult.of("사용자를 차단했습니다.");
            }
            userAuditPublisher.userBlockSkippedDuplicate(blockerId, targetUserId);
            return BlockOperationResult.of("이미 차단된 사용자입니다.");
        } catch (IllegalArgumentException | IllegalStateException | NoSuchElementException e) {
            return handleBlockFailure(blockerId, targetUserId, e, "차단 처리 중 오류가 발생했습니다.");
        } catch (RuntimeException e) {
            userAuditPublisher.userBlockFailed(blockerId, targetUserId, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public BlockOperationResult unblock(Long blockerId, Long targetUserId) {
        try {
            boolean removed = userBlockService.unblock(blockerId, targetUserId);
            if (removed) {
                userAuditPublisher.userBlockRemove(blockerId, targetUserId);
                return BlockOperationResult.of("사용자 차단을 해제했습니다.");
            }
            userAuditPublisher.userUnblockSkippedNotFound(blockerId, targetUserId);
            return BlockOperationResult.of("차단 내역이 없어 변경하지 않았습니다.");
        } catch (IllegalArgumentException | IllegalStateException | NoSuchElementException e) {
            return handleUnblockFailure(blockerId, targetUserId, e, "차단 해제 처리 중 오류가 발생했습니다.");
        } catch (RuntimeException e) {
            userAuditPublisher.userUnblockFailed(blockerId, targetUserId, e.getMessage(), e);
            throw e;
        }
    }

    private BlockOperationResult handleBlockFailure(Long blockerId,
                                                    Long targetUserId,
                                                    RuntimeException e,
                                                    String fallbackMessage) {
        userAuditPublisher.userBlockFailed(blockerId, targetUserId, e.getMessage(), e);
        return BlockOperationResult.of(resolveMessage(e, fallbackMessage));
    }

    private BlockOperationResult handleUnblockFailure(Long blockerId,
                                                      Long targetUserId,
                                                      RuntimeException e,
                                                      String fallbackMessage) {
        userAuditPublisher.userUnblockFailed(blockerId, targetUserId, e.getMessage(), e);
        return BlockOperationResult.of(resolveMessage(e, fallbackMessage));
    }

    private String resolveMessage(RuntimeException e, String fallbackMessage) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            return fallbackMessage;
        }
        return message;
    }

    public record BlockOperationResult(String message) {

        public static BlockOperationResult of(String message) {
            return new BlockOperationResult(message);
        }
    }
}
