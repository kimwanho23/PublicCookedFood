package kwh.PublicCookedFood.user.service;

import kwh.PublicCookedFood.user.domain.Role;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void findByNameAndPhoneNumber_normalizesPhoneNumberAndTrimsName() {
        Users user = createUser(10L, "recover@test.com");
        when(userRepository.findByNameAndPhoneNumber("홍길동", "01012345678"))
                .thenReturn(Optional.of(user));

        Optional<Users> result = userService.findByNameAndPhoneNumber(" 홍길동 ", "010-1234-5678");

        assertThat(result).contains(user);
        verify(userRepository).findByNameAndPhoneNumber("홍길동", "01012345678");
    }

    @Test
    void findByNameAndPhoneNumber_returnsEmptyWhenInputIsBlank() {
        Optional<Users> result = userService.findByNameAndPhoneNumber(" ", " ");

        assertThat(result).isEmpty();
        verifyNoInteractions(userRepository);
    }

    @Test
    void findByEmailAndNameAndPhoneNumber_normalizesAllInputs() {
        Users user = createUser(22L, "recover2@test.com");
        when(userRepository.findByEmailAndNameAndPhoneNumber("recover2@test.com", "홍길동", "01022223333"))
                .thenReturn(Optional.of(user));

        Optional<Users> result = userService.findByEmailAndNameAndPhoneNumber(
                " recover2@test.com ",
                " 홍길동 ",
                "010-2222-3333");

        assertThat(result).contains(user);
        verify(userRepository).findByEmailAndNameAndPhoneNumber("recover2@test.com", "홍길동", "01022223333");
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
