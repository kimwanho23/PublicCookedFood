package kwh.PublicCookedFood.user.service;

import kwh.PublicCookedFood.user.domain.Role;
import kwh.PublicCookedFood.user.domain.UserBlock;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.repository.UserBlockRepository;
import kwh.PublicCookedFood.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserBlockServiceUnitTest {

    @Mock
    private UserBlockRepository userBlockRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserBlockService userBlockService;

    @Test
    void block_returnsFalseWhenAlreadyBlocked() {
        when(userBlockRepository.existsByBlockerIdAndBlockedId(1L, 2L)).thenReturn(true);

        boolean created = userBlockService.block(1L, 2L);

        assertThat(created).isFalse();
        verify(userBlockRepository, never()).save(any(UserBlock.class));
    }

    @Test
    void block_throwsWhenSelfBlockRequested() {
        assertThatThrownBy(() -> userBlockService.block(1L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("본인은 차단");
    }

    @Test
    void unblock_returnsFalseWhenNoRowDeleted() {
        when(userBlockRepository.deleteByBlockerIdAndBlockedId(1L, 2L)).thenReturn(0L);

        boolean removed = userBlockService.unblock(1L, 2L);

        assertThat(removed).isFalse();
    }

    @Test
    void block_returnsFalseWhenDuplicateConstraintRaisedByRaceCondition() {
        Users blocker = createUser(1L, "blocker@test.com");
        Users blocked = createUser(2L, "blocked@test.com");
        when(userBlockRepository.existsByBlockerIdAndBlockedId(1L, 2L)).thenReturn(false, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(blocker));
        when(userRepository.findById(2L)).thenReturn(Optional.of(blocked));
        when(userBlockRepository.saveAndFlush(any(UserBlock.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        boolean created = userBlockService.block(1L, 2L);

        assertThat(created).isFalse();
    }

    @Test
    void getBlockedUsers_returnsBlockedUsers() {
        Users blocker = createUser(1L, "blocker@test.com");
        Users blockedA = createUser(2L, "blocked-a@test.com");
        Users blockedB = createUser(3L, "blocked-b@test.com");

        when(userBlockRepository.findByBlockerIdOrderByRegTimeDesc(1L)).thenReturn(List.of(
                UserBlock.of(blocker, blockedA),
                UserBlock.of(blocker, blockedB)));

        List<Users> blockedUsers = userBlockService.getBlockedUsers(1L);

        assertThat(blockedUsers).extracting(Users::getId).containsExactlyInAnyOrder(2L, 3L);
        verify(userBlockRepository).findByBlockerIdOrderByRegTimeDesc(1L);
    }

    private Users createUser(Long id, String email) {
        return Users.builder()
                .id(id)
                .email(email)
                .name("테스터")
                .authority(Role.USER)
                .loginMethod("Current")
                .build();
    }
}
