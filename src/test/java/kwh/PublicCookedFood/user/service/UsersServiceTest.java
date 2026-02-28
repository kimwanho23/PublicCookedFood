package kwh.PublicCookedFood.user.service;

import kwh.PublicCookedFood.user.domain.Role;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsersServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("회원가입 테스트")
    void saveMemberTest() {
        Users users = createUser("test@email.com");
        when(userRepository.findByEmail("test@email.com")).thenReturn(Optional.empty());
        when(userRepository.saveAndFlush(users)).thenReturn(users);

        Users savedMember = userService.save(users);

        assertThat(savedMember.getEmail()).isEqualTo("test@email.com");
        verify(userRepository).findByEmail("test@email.com");
        verify(userRepository).saveAndFlush(users);
    }

    @Test
    void saveMemberTest_throwsWhenDuplicateEmailExists() {
        Users existing = createUser("dup@email.com");
        Users incoming = createUser("dup@email.com");
        when(userRepository.findByEmail("dup@email.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> userService.save(incoming))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 가입된 이메일");
    }

    private Users createUser(String email) {
        return Users.builder()
                .email(email)
                .name("홍길동")
                .password("encoded-password")
                .authority(Role.USER)
                .loginMethod("Current")
                .build();
    }
}
