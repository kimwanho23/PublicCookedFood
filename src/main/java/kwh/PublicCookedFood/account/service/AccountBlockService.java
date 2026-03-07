package kwh.PublicCookedFood.account.service;

import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.account.domain.AccountBlock;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.error.AccountErrorCode;
import kwh.PublicCookedFood.account.repository.AccountBlockRepository;
import kwh.PublicCookedFood.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AccountBlockService {

    private final AccountBlockRepository accountBlockRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public boolean block(Long blockerId, Long blockedId) {
        validateBlockRequest(blockerId, blockedId);
        if (accountBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            return false;
        }

        Account blocker = getAccountById(blockerId);
        Account blocked = getAccountById(blockedId);
        try {
            accountBlockRepository.saveAndFlush(AccountBlock.of(blocker, blocked));
            return true;
        } catch (DataIntegrityViolationException e) {
            if (accountBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
                return false;
            }
            throw e;
        }
    }

    @Transactional
    public boolean unblock(Long blockerId, Long blockedId) {
        if (blockerId == null || blockedId == null) {
            return false;
        }
        long deletedCount = accountBlockRepository.deleteByBlockerIdAndBlockedId(blockerId, blockedId);
        return deletedCount > 0;
    }

    @Transactional(readOnly = true)
    public boolean isBlocked(Long blockerId, Long blockedId) {
        if (blockerId == null || blockedId == null || blockerId.equals(blockedId)) {
            return false;
        }
        return accountBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId);
    }

    @Transactional(readOnly = true)
    public boolean isEitherBlocked(Long accountAId, Long accountBId) {
        if (accountAId == null || accountBId == null || accountAId.equals(accountBId)) {
            return false;
        }
        return accountBlockRepository.existsBetweenAccounts(accountAId, accountBId);
    }

    @Transactional(readOnly = true)
    public Set<Long> getViewRestrictedAccountIds(Long accountId) {
        if (accountId == null) {
            return Collections.emptySet();
        }

        Set<Long> restrictedAccountIds = new HashSet<>();
        accountBlockRepository.findBlockedIdsByBlockerId(accountId).stream()
                .forEach(restrictedAccountIds::add);
        accountBlockRepository.findBlockerIdsByBlockedId(accountId).stream()
                .forEach(restrictedAccountIds::add);
        restrictedAccountIds.remove(accountId);
        return Set.copyOf(restrictedAccountIds);
    }

    @Transactional(readOnly = true)
    public Set<Long> getRestrictedCounterpartyIds(Long accountId, Set<Long> candidateIds) {
        if (accountId == null || candidateIds == null || candidateIds.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> normalizedCandidateIds = candidateIds.stream()
                .filter(id -> id != null && !id.equals(accountId))
                .collect(java.util.stream.Collectors.toSet());
        if (normalizedCandidateIds.isEmpty()) {
            return Collections.emptySet();
        }
        return Set.copyOf(accountBlockRepository.findRestrictedCounterpartyIds(accountId, normalizedCandidateIds));
    }

    @Transactional(readOnly = true)
    public List<Account> getBlockedAccounts(Long blockerId) {
        if (blockerId == null) {
            return List.of();
        }
        return accountBlockRepository.findByBlockerIdOrderByRegTimeDesc(blockerId).stream()
                .map(AccountBlock::getBlocked)
                .toList();
    }

    private void validateBlockRequest(Long blockerId, Long blockedId) {
        if (blockerId == null || blockedId == null) {
            throw new AppException(AccountErrorCode.ACCOUNT_BLOCK_INVALID_REQUEST);
        }
        if (blockerId.equals(blockedId)) {
            throw new AppException(AccountErrorCode.ACCOUNT_BLOCK_SELF_FORBIDDEN);
        }
    }

    private Account getAccountById(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AppException(AccountErrorCode.ACCOUNT_NOT_FOUND));
    }
}


