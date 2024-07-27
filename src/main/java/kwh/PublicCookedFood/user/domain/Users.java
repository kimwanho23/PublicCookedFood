package kwh.PublicCookedFood.user.domain;

import jakarta.persistence.*;
import kwh.PublicCookedFood.user.dto.UserSaveDto;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.Serializable;

@Entity
@Getter
@Table(name = "user")
public class Users {

    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email; //이메일

    @Column
    private String password; //비밀번호

    @Column(nullable = false)
    private String name; //별명

    @Enumerated(EnumType.STRING)
    private Role authority; // 권한

    @Column(nullable = false)
    private String loginMethod;

    public Users() {
    }

    @Builder
    public Users(Long id, String email, String password, String name, Role authority, String loginMethod) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.name = name;
        this.authority = authority;
        this.loginMethod = loginMethod;
    }

    public Users update(String name) {
        this.name = name;
        return this;
    }

    public String getRoleKey() {
        return this.authority.getKey();
    }

    public static Users createUser(UserSaveDto userSaveDto,
                                   PasswordEncoder passwordEncoder) {
        return Users.builder()
                .name(userSaveDto.getName())
                .email(userSaveDto.getEmail())
                .password(passwordEncoder.encode(userSaveDto.getPassword()))
                .authority(Role.USER)
                .loginMethod("Current")
                .build();
    }

}
