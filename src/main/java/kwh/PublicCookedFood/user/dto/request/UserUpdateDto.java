package kwh.PublicCookedFood.user.dto.request;

import jakarta.validation.constraints.Size;
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

    @Size(max = 20)
    private String name;

    @Builder
    public UserUpdateDto(String email, String password, String name) {
        this.email = email;
        this.password = password;
        this.name = name;
    }
}
