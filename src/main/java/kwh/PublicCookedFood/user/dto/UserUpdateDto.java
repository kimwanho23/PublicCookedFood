package kwh.PublicCookedFood.user.dto;

import kwh.PublicCookedFood.user.domain.Users;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
public class UserUpdateDto {

    private String email;

    private String password;

    private String name;

    @Builder
    public UserUpdateDto(String email, String password, String name) {
        this.email = email;
        this.password = password;
        this.name = name;
    }

    public Users toEntity(){
        return Users.builder()
                .email(email)
                .password(password)
                .name(name).build();
    }
}
