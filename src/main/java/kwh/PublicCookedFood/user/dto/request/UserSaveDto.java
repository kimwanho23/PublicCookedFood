package kwh.PublicCookedFood.user.dto.request;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;


@Getter
public class UserSaveDto {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, max = 50)
    private String password;

    @NotBlank
    @Size(max = 20)
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
