package kwh.PublicCookedFood.user.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Getter;

@Getter
public class LoginDto {

    @NotEmpty
    private String email;

    @NotEmpty
    private String password;

    @Builder
    public LoginDto(String email, String password) {
        this.email = email;
        this.password = password;
    }
}
