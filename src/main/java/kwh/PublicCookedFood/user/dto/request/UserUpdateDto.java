package kwh.PublicCookedFood.user.dto.request;

import jakarta.validation.constraints.Size;
import kwh.PublicCookedFood.user.domain.Gender;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;


@Getter
@Setter
@NoArgsConstructor
public class UserUpdateDto {

    private String email;

    private String password;

    @Size(max = 20)
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

    @Builder
    public UserUpdateDto(String email, String password, String name, String phoneNumber, LocalDate birthDate,
                         Gender gender, String address, String addressDetail, String profileImageUrl) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.birthDate = birthDate;
        this.gender = gender;
        this.address = address;
        this.addressDetail = addressDetail;
        this.profileImageUrl = profileImageUrl;
    }
}
