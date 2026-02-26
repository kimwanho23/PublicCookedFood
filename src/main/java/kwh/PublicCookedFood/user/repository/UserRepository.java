package kwh.PublicCookedFood.user.repository;

import kwh.PublicCookedFood.user.domain.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<Users, Long> {
    Optional<Users> findByEmail(String email);

    Optional<Users> findByNameAndPhoneNumber(String name, String phoneNumber);

    Optional<Users> findByEmailAndNameAndPhoneNumber(String email, String name, String phoneNumber);

    List<Users> findByName(String name);
}
