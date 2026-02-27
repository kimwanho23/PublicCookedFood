package kwh.PublicCookedFood.user.policy;

import kwh.PublicCookedFood.user.domain.Role;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.service.UserBlockService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileAccessPolicyUnitTest {

    @Mock
    private UserBlockService userBlockService;

    @InjectMocks
    private UserProfileAccessPolicy userProfileAccessPolicy;

    @Test
    void isProfileViewRestricted_returnsFalseForSelfProfile() {
        Users viewer = createUser(11L);

        boolean restricted = userProfileAccessPolicy.isProfileViewRestricted(viewer, 11L);

        assertThat(restricted).isFalse();
    }

    @Test
    void isProfileViewRestricted_delegatesToBlockServiceForOtherUser() {
        Users viewer = createUser(11L);
        when(userBlockService.isEitherBlocked(11L, 22L)).thenReturn(true);

        boolean restricted = userProfileAccessPolicy.isProfileViewRestricted(viewer, 22L);

        assertThat(restricted).isTrue();
        verify(userBlockService).isEitherBlocked(11L, 22L);
    }

    @Test
    void hasBlockedProfileUser_delegatesToBlockServiceForOtherUser() {
        Users viewer = createUser(11L);
        when(userBlockService.isBlocked(11L, 22L)).thenReturn(true);

        boolean blocked = userProfileAccessPolicy.hasBlockedProfileUser(viewer, 22L);

        assertThat(blocked).isTrue();
        verify(userBlockService).isBlocked(11L, 22L);
    }

    @Test
    void hasBlockedProfileUser_returnsFalseForNullViewer() {
        boolean blocked = userProfileAccessPolicy.hasBlockedProfileUser(null, 22L);

        assertThat(blocked).isFalse();
    }

    private Users createUser(Long id) {
        return Users.builder()
                .id(id)
                .email("viewer-" + id + "@test.com")
                .name("테스터")
                .authority(Role.USER)
                .loginMethod("Current")
                .build();
    }
}
