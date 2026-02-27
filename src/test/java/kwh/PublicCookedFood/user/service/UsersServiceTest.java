package kwh.PublicCookedFood.user.service;

import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.dto.request.UserSaveDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;


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
        UserSaveDto userDto = new UserSaveDto();
        userDto.setEmail("test@email.com");
        userDto.setName("홍길동");
        userDto.setPassword("12345678");
        return Users.createUser(userDto, passwordEncoder);
    }

    @Test
    @DisplayName("회원가입 테스트")
    public void saveMemberTest() {
        Users users = createUser();
        Users savedMember = userService.save(users);

        assertThat(users.getEmail()).isEqualTo(savedMember.getEmail());
    }
}
