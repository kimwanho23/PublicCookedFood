package kwh.PublicCookedFood.user.facade;

import jakarta.annotation.PostConstruct;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.dto.request.UserUpdateDto;
import kwh.PublicCookedFood.user.audit.UserAuditPublisher;
import kwh.PublicCookedFood.user.facade.profile.UserProfileViewPages;
import kwh.PublicCookedFood.user.facade.profile.UserProfileViewStrategy;
import kwh.PublicCookedFood.user.facade.profile.UserProfileViewType;
import kwh.PublicCookedFood.user.policy.UserProfileAccessPolicy;
import kwh.PublicCookedFood.user.service.UserBlockService;
import kwh.PublicCookedFood.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserProfileFacade {

    private final UserService userService;
    private final UserBlockService userBlockService;
    private final UserAuditPublisher userAuditPublisher;
    private final UserProfileAccessPolicy userProfileAccessPolicy;
    private final List<UserProfileViewStrategy> profileViewStrategies;

    private Map<UserProfileViewType, UserProfileViewStrategy> profileViewStrategyMap = Map.of();

    @PostConstruct
    void initializeProfileViewStrategies() {
        EnumMap<UserProfileViewType, UserProfileViewStrategy> strategyMap = new EnumMap<>(UserProfileViewType.class);
        for (UserProfileViewStrategy strategy : profileViewStrategies) {
            UserProfileViewType viewType = strategy.viewType();
            UserProfileViewStrategy existing = strategyMap.putIfAbsent(viewType, strategy);
            if (existing != null) {
                throw new IllegalStateException("중복된 프로필 뷰 전략이 등록되어 있습니다: " + viewType.key());
            }
        }

        for (UserProfileViewType viewType : UserProfileViewType.values()) {
            if (!strategyMap.containsKey(viewType)) {
                throw new IllegalStateException("필수 프로필 뷰 전략이 누락되었습니다: " + viewType.key());
            }
        }

        profileViewStrategyMap = Map.copyOf(strategyMap);
    }

    public void populateUserUpdateDto(UserUpdateDto userUpdateDto, Users user) {
        if (userUpdateDto.getEmail() == null) {
            userUpdateDto.setEmail(user.getEmail());
        }
        if (userUpdateDto.getName() == null) {
            userUpdateDto.setName(user.getName());
        }
        if (userUpdateDto.getPhoneNumber() == null) {
            userUpdateDto.setPhoneNumber(user.getPhoneNumber());
        }
        if (userUpdateDto.getBirthDate() == null) {
            userUpdateDto.setBirthDate(user.getBirthDate());
        }
        if (userUpdateDto.getGender() == null) {
            userUpdateDto.setGender(user.getGender());
        }
        if (userUpdateDto.getAddress() == null) {
            userUpdateDto.setAddress(user.getAddress());
        }
        if (userUpdateDto.getAddressDetail() == null) {
            userUpdateDto.setAddressDetail(user.getAddressDetail());
        }
        if (userUpdateDto.getProfileImageUrl() == null) {
            userUpdateDto.setProfileImageUrl(user.getProfileImageUrl());
        }
    }

    public List<Users> getBlockedUsers(Long userId) {
        return userBlockService.getBlockedUsers(userId);
    }

    public OtherProfileViewData loadOtherProfile(Long userId,
                                                 String view,
                                                 Users loginUser,
                                                 Pageable pageable) {
        Users profileUser = userService.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자 정보를 찾을 수 없습니다."));

        if (userProfileAccessPolicy.isProfileViewRestricted(loginUser, userId)) {
            throw new IllegalStateException("차단 관계인 사용자의 프로필은 조회할 수 없습니다.");
        }

        Set<Long> blockedUserIds = userBlockService.getViewRestrictedUserIds(loginUser == null ? null : loginUser.getId());
        UserProfileViewType profileViewType = UserProfileViewType.from(view);
        UserProfileViewPages viewPages = resolveProfileViewStrategy(profileViewType)
                .load(userId, pageable, blockedUserIds);

        boolean myBlockedProfileUser = userProfileAccessPolicy.hasBlockedProfileUser(loginUser, userId);
        Long currentUserId = loginUser == null ? null : loginUser.getId();

        return new OtherProfileViewData(
                profileUser,
                profileViewType.key(),
                currentUserId,
                myBlockedProfileUser,
                viewPages.boardPage(),
                viewPages.commentPage(),
                viewPages.scrapPage()
        );
    }

    @Transactional
    public ProfileUpdateResult updateProfile(Users user, UserUpdateDto userUpdateDto) {
        Users existingUser = userService.findUserByEmail(user.getEmail());
        if (existingUser == null) {
            userAuditPublisher.userProfileUpdateFailedUserNotFound(user.getId());
            return ProfileUpdateResult.failure("사용자 정보를 찾을 수 없습니다.");
        }

        try {
            existingUser.updateProfile(
                    userUpdateDto.getName(),
                    userUpdateDto.getPhoneNumber(),
                    userUpdateDto.getBirthDate(),
                    userUpdateDto.getGender(),
                    userUpdateDto.getAddress(),
                    userUpdateDto.getAddressDetail(),
                    userUpdateDto.getProfileImageUrl()
            );

            Users savedUser = userService.save(existingUser);
            userAuditPublisher.userProfileUpdate(savedUser.getId());
            return ProfileUpdateResult.success(savedUser);
        } catch (IllegalStateException e) {
            userAuditPublisher.userProfileUpdateFailed(user.getId(), e.getMessage());
            return ProfileUpdateResult.failure(e.getMessage());
        }
    }

    private UserProfileViewStrategy resolveProfileViewStrategy(UserProfileViewType profileViewType) {
        UserProfileViewStrategy strategy = profileViewStrategyMap.get(profileViewType);
        if (strategy == null) {
            throw new IllegalStateException("지원하지 않는 프로필 뷰 전략입니다: " + profileViewType.key());
        }
        return strategy;
    }

    public record OtherProfileViewData(Users profileUser,
                                       String profileView,
                                       Long currentUserId,
                                       boolean myBlockedProfileUser,
                                       Page<Board> boardPage,
                                       Page<Comments> commentPage,
                                       Page<Board> scrapPage) {

        public Page<?> activePage() {
            UserProfileViewType viewType = UserProfileViewType.from(profileView);
            return switch (viewType) {
                case COMMENTS -> commentPage;
                case SCRAPS -> scrapPage;
                case BOARDS -> boardPage;
            };
        }
    }

    public record ProfileUpdateResult(boolean success,
                                      Users savedUser,
                                      String errorMessage) {

        public static ProfileUpdateResult success(Users savedUser) {
            return new ProfileUpdateResult(true, savedUser, null);
        }

        public static ProfileUpdateResult failure(String errorMessage) {
            return new ProfileUpdateResult(false, null, errorMessage);
        }
    }
}
