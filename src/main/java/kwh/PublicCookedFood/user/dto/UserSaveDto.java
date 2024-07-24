package kwh.PublicCookedFood.user.dto;

import jakarta.validation.constraints.*;
import kwh.PublicCookedFood.user.domain.Users;
import lombok.Builder;
import lombok.Getter;


@Getter
public class UserSaveDto {

    @NotEmpty(message = "아이디를 입력하세요")
    private String loginId;

    @NotEmpty(message = "비밀번호를 입력하세요")
    private String password;

    @NotEmpty(message = "별명을 입력하세요")
    private String name;

    @Email(message = "올바른 이메일 주소를 입력해주세요")
    @NotEmpty(message = "이메일을 입력하세요")
    private String email;

    @Builder
    public UserSaveDto(String loginId, String password, String name, String email) {
        this.loginId = loginId;
        this.password = password;
        this.name = name;
        this.email = email;
    }
}
