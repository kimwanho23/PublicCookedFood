package kwh.PublicCookedFood.account.repository;

import kwh.PublicCookedFood.account.domain.AccountActivityLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AccountActivityLogRepository extends JpaRepository<AccountActivityLog, Long> {

    @Query("SELECT l FROM AccountActivityLog l JOIN FETCH l.account ORDER BY l.regTime DESC")
    List<AccountActivityLog> findRecentWithAccount(Pageable pageable);
}
