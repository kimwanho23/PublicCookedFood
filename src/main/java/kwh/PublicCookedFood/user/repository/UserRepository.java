package kwh.PublicCookedFood.user.repository;

import kwh.PublicCookedFood.user.domain.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<Users, Long> {

    Users findByLoginId(String loginId);
}
