package kwh.PublicCookedFood.user.service;

import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.dto.UserSaveDto;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class UsersServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    PasswordEncoder passwordEncoder;


    @AfterEach
    public void clearStore() {

    }

    public Users createUser() {
        UserSaveDto userDto = UserSaveDto.builder()
                .email("test@email.com")
                .name("홍길동")
                .password("12345678")
                .build();
        return Users.createUser(userDto, passwordEncoder);
    }

    @Test
    @DisplayName("회원가입 테스트")
    public void saveMemberTest() {
        Users users = createUser();
        Users savedMember = userService.save(users);

        assertThat(users.getEmail()).isEqualTo(savedMember.getEmail());
    }


/*    @Test
    void loginTest(){ //로그인이 되었는가?
        UserSaveDto userDto = UserSaveDto.builder()
                .loginId("test12")
                .name("홍길동")
                .email("test@email.com")
                .password("12345678")
                .build();
        Optional<Users> login = userService.login(userDto.getLoginId(), userDto.getPassword(), passwordEncoder);
        System.out.println(userDto.getLoginId());
        System.out.println(userDto.getPassword());
        Assertions.assertThat(login.isPresent()).isTrue();

    }*/

}