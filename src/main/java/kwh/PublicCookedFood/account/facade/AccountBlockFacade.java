package kwh.PublicCookedFood.account.facade;

import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.common.error.ErrorMessageResolver;
import kwh.PublicCookedFood.account.audit.AccountAuditPublisher;
import kwh.PublicCookedFood.account.service.AccountBlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountBlockFacade {

    private final AccountBlockService accountBlockService;
    private final AccountAuditPublisher accountAuditPublisher;

    @Transactional(readOnly = true)
    public void auditBlockFailedUnauthenticated(Long targetAccountId) {
        accountAuditPublisher.accountBlockFailedUnauthenticated(targetAccountId);
    }

    @Transactional(readOnly = true)
    public void auditUnblockFailedUnauthenticated(Long targetAccountId) {
        accountAuditPublisher.accountUnblockFailedUnauthenticated(targetAccountId);
    }

    @Transactional
    public BlockOperationResult block(Long blockerId, Long targetAccountId) {
        try {
            boolean created = accountBlockService.block(blockerId, targetAccountId);
            if (created) {
                accountAuditPublisher.accountBlockAdd(blockerId, targetAccountId);
                return BlockOperationResult.of("사용자를 차단했습니다.");
            }
            accountAuditPublisher.accountBlockSkippedDuplicate(blockerId, targetAccountId);
            return BlockOperationResult.of("이미 차단한 사용자입니다.");
        } catch (AppException e) {
            return handleBlockFailure(blockerId, targetAccountId, e, "차단 처리 중 오류가 발생했습니다.");
        } catch (RuntimeException e) {
            accountAuditPublisher.accountBlockFailed(blockerId, targetAccountId, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public BlockOperationResult unblock(Long blockerId, Long targetAccountId) {
        try {
            boolean removed = accountBlockService.unblock(blockerId, targetAccountId);
            if (removed) {
                accountAuditPublisher.accountBlockRemove(blockerId, targetAccountId);
                return BlockOperationResult.of("사용자 차단을 해제했습니다.");
            }
            accountAuditPublisher.accountUnblockSkippedNotFound(blockerId, targetAccountId);
            return BlockOperationResult.of("차단 이력이 없어 변경하지 않았습니다.");
        } catch (AppException e) {
            return handleUnblockFailure(blockerId, targetAccountId, e, "차단 해제 처리 중 오류가 발생했습니다.");
        } catch (RuntimeException e) {
            accountAuditPublisher.accountUnblockFailed(blockerId, targetAccountId, e.getMessage(), e);
            throw e;
        }
    }

    private BlockOperationResult handleBlockFailure(Long blockerId,
                                                    Long targetAccountId,
                                                    RuntimeException e,
                                                    String fallbackMessage) {
        accountAuditPublisher.accountBlockFailed(blockerId, targetAccountId, e.getMessage(), e);
        return BlockOperationResult.of(ErrorMessageResolver.resolve(e, fallbackMessage));
    }

    private BlockOperationResult handleUnblockFailure(Long blockerId,
                                                      Long targetAccountId,
                                                      RuntimeException e,
                                                      String fallbackMessage) {
        accountAuditPublisher.accountUnblockFailed(blockerId, targetAccountId, e.getMessage(), e);
        return BlockOperationResult.of(ErrorMessageResolver.resolve(e, fallbackMessage));
    }

    public record BlockOperationResult(String message) {

        public static BlockOperationResult of(String message) {
            return new BlockOperationResult(message);
        }
    }
}

