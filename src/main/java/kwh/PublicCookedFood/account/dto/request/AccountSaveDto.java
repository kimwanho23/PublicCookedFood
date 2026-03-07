package kwh.PublicCookedFood.account.dto.request;

import jakarta.validation.constraints.*;
import kwh.PublicCookedFood.account.domain.Gender;
import kwh.PublicCookedFood.account.policy.AccountNicknamePolicy;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;


@Getter
@Setter
@NoArgsConstructor
public class AccountSaveDto {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, max = 50)
    private String password;

    @NotBlank
    @Size(min = AccountNicknamePolicy.MIN_LENGTH, max = AccountNicknamePolicy.MAX_LENGTH)
    @Pattern(regexp = AccountNicknamePolicy.REGEX, message = "닉네임은 2~20자의 한글, 영문, 숫자, 밑줄(_), 하이픈(-)만 사용할 수 있습니다.")
    private String name;

    @NotBlank
    @Pattern(regexp = "^[0-9\\-]{9,20}$", message = "전화번호 형식이 올바르지 않습니다.")
    private String phoneNumber;

    @NotNull
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Past(message = "생년월일은 오늘 이전 날짜여야 합니다.")
    private LocalDate birthDate;

    @NotNull
    private Gender gender;

    @Size(max = 120)
    private String address;

    @Size(max = 120)
    private String addressDetail;

    @Size(max = 500)
    private String profileImageUrl;

    private String loginMethod;
}
