package kwh.PublicCookedFood.account.repository;

import kwh.PublicCookedFood.account.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByEmail(String email);

    Optional<Account> findByPhoneNumberAndBirthDate(String phoneNumber, java.time.LocalDate birthDate);

    Optional<Account> findByEmailAndPhoneNumberAndBirthDate(String email, String phoneNumber, java.time.LocalDate birthDate);

    Optional<Account> findByName(String name);

    boolean existsByProfileImageUrl(String profileImageUrl);
}
