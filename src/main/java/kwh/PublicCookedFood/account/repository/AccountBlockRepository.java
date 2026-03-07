package kwh.PublicCookedFood.account.repository;

import kwh.PublicCookedFood.account.domain.AccountBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountBlockRepository extends JpaRepository<AccountBlock, Long> {

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    Optional<AccountBlock> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    long deleteByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    List<AccountBlock> findByBlockerId(Long blockerId);

    List<AccountBlock> findByBlockerIdOrderByRegTimeDesc(Long blockerId);

    List<AccountBlock> findByBlockedId(Long blockedId);
}
