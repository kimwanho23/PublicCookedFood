package kwh.PublicCookedFood.user.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.validator.constraints.UniqueElements;


@Getter
public class UserSaveDto {

    @Email(message = "올바른 이메일 주소를 입력해주세요")
    @NotEmpty(message = "이메일을 입력하세요")
    private String email;

    private String password;

    @NotEmpty(message = "별명을 입력하세요")
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
