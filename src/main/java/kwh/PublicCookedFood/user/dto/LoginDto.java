package kwh.PublicCookedFood.user.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Getter;

@Getter
public class LoginDto {

    @NotEmpty
    private String loginId;

    @NotEmpty
    private String password;

    @Builder
    public LoginDto(String loginId, String password) {
        this.loginId = loginId;
        this.password = password;
    }
}
