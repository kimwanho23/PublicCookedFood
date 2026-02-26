package kwh.PublicCookedFood.user.service;

import kwh.PublicCookedFood.user.domain.UserActivityLog;
import kwh.PublicCookedFood.user.repository.UserActivityLogRepository;
import kwh.PublicCookedFood.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserActivityLogService {

    private static final int DEFAULT_RECENT_LIMIT = 10;
    private static final int MAX_RECENT_LIMIT = 50;

    private final UserActivityLogRepository userActivityLogRepository;
    private final UserRepository userRepository;

    @Transactional
    public void record(Long userId, String action, String detail) {
        if (userId == null || action == null || action.isBlank()) {
            return;
        }
        userRepository.findById(userId).ifPresent(user -> userActivityLogRepository.save(
                UserActivityLog.builder()
                        .user(user)
                        .action(action)
                        .detail(detail)
                        .build()
        ));
    }

    @Transactional(readOnly = true)
    public List<UserActivityLog> getRecentActivities(int limit) {
        int normalizedLimit = limit <= 0 ? DEFAULT_RECENT_LIMIT : Math.min(limit, MAX_RECENT_LIMIT);
        return userActivityLogRepository.findRecentWithUser(PageRequest.of(0, normalizedLimit));
    }
}
