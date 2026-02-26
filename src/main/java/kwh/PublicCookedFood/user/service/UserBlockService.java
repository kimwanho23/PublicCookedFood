package kwh.PublicCookedFood.user.service;

import kwh.PublicCookedFood.user.domain.UserBlock;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.repository.UserBlockRepository;
import kwh.PublicCookedFood.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserBlockService {

    private final UserBlockRepository userBlockRepository;
    private final UserRepository userRepository;

    @Transactional
    public boolean block(Long blockerId, Long blockedId) {
        validateBlockRequest(blockerId, blockedId);
        if (userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            return false;
        }

        Users blocker = getUserById(blockerId);
        Users blocked = getUserById(blockedId);
        try {
            userBlockRepository.save(UserBlock.of(blocker, blocked));
            log.info("action=user.block result=created blockerId={} blockedId={}", blockerId, blockedId);
            return true;
        } catch (DataIntegrityViolationException e) {
            if (userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
                log.info("action=user.block result=skipped_duplicate blockerId={} blockedId={}", blockerId, blockedId);
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
        long deletedCount = userBlockRepository.deleteByBlockerIdAndBlockedId(blockerId, blockedId);
        if (deletedCount > 0) {
            log.info("action=user.unblock result=removed blockerId={} blockedId={}", blockerId, blockedId);
            return true;
        }
        return false;
    }

    @Transactional(readOnly = true)
    public boolean isBlocked(Long blockerId, Long blockedId) {
        if (blockerId == null || blockedId == null || blockerId.equals(blockedId)) {
            return false;
        }
        return userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId);
    }

    @Transactional(readOnly = true)
    public boolean isEitherBlocked(Long userAId, Long userBId) {
        if (userAId == null || userBId == null || userAId.equals(userBId)) {
            return false;
        }
        return userBlockRepository.existsByBlockerIdAndBlockedId(userAId, userBId)
                || userBlockRepository.existsByBlockerIdAndBlockedId(userBId, userAId);
    }

    @Transactional(readOnly = true)
    public Set<Long> getBlockedUserIds(Long blockerId) {
        if (blockerId == null) {
            return Collections.emptySet();
        }
        return userBlockRepository.findByBlockerId(blockerId).stream()
                .map(userBlock -> userBlock.getBlocked().getId())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Transactional(readOnly = true)
    public Set<Long> getViewRestrictedUserIds(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }

        Set<Long> restrictedUserIds = new HashSet<>();
        userBlockRepository.findByBlockerId(userId).stream()
                .map(userBlock -> userBlock.getBlocked().getId())
                .forEach(restrictedUserIds::add);
        userBlockRepository.findByBlockedId(userId).stream()
                .map(userBlock -> userBlock.getBlocker().getId())
                .forEach(restrictedUserIds::add);
        restrictedUserIds.remove(userId);
        return Set.copyOf(restrictedUserIds);
    }

    @Transactional(readOnly = true)
    public List<Users> getBlockedUsers(Long blockerId) {
        if (blockerId == null) {
            return List.of();
        }
        return userBlockRepository.findByBlockerIdOrderByRegTimeDesc(blockerId).stream()
                .map(UserBlock::getBlocked)
                .toList();
    }

    private void validateBlockRequest(Long blockerId, Long blockedId) {
        if (blockerId == null || blockedId == null) {
            throw new IllegalArgumentException("차단 대상 정보가 올바르지 않습니다.");
        }
        if (blockerId.equals(blockedId)) {
            throw new IllegalArgumentException("본인은 차단할 수 없습니다.");
        }
    }

    private Users getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자 정보를 찾을 수 없습니다."));
    }
}
