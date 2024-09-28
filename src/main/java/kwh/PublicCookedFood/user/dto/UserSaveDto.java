package kwh.PublicCookedFood.user.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;


@Getter
public class UserSaveDto {
    private String email;

    private String password;

    private String name;

    private String loginMethod;


    @Builder
    public UserSaveDto(String email, String password, String name,  String loginMethod) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.loginMethod = loginMethod;
    }
}
