package kwh.PublicCookedFood.user.repository;

import kwh.PublicCookedFood.user.domain.UserActivityLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserActivityLogRepository extends JpaRepository<UserActivityLog, Long> {

    @Query("SELECT l FROM UserActivityLog l JOIN FETCH l.user ORDER BY l.regTime DESC")
    List<UserActivityLog> findRecentWithUser(Pageable pageable);
}
