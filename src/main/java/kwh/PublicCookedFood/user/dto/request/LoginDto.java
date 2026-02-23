package kwh.PublicCookedFood.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

@Getter
public class LoginDto {

    @NotEmpty
    @Email
    @Size(max = 100)
    private String email;

    @NotEmpty
    @Size(max = 100)
    private String password;

    @Builder
    public LoginDto(String email, String password) {
        this.email = email;
        this.password = password;
    }
}
