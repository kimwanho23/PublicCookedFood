package kwh.PublicCookedFood.account.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
public class AccountUpdateDto {

    private String email;

    private String password;

    @Size(min = AccountNicknamePolicy.MIN_LENGTH, max = AccountNicknamePolicy.MAX_LENGTH)
    @Pattern(regexp = AccountNicknamePolicy.REGEX, message = "닉네임은 2~20자의 한글, 영문, 숫자, 밑줄(_), 하이픈(-)만 사용할 수 있습니다.")
    private String name;

    @Size(max = 30)
    private String phoneNumber;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    private Gender gender;

    @Size(max = 120)
    private String address;

    @Size(max = 120)
    private String addressDetail;

    @Size(max = 500)
    private String profileImageUrl;
}
