package kwh.PublicCookedFood.account.repository;

import kwh.PublicCookedFood.account.domain.AccountBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
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

    @Query("SELECT ab.blocked.id FROM AccountBlock ab WHERE ab.blocker.id = :blockerId")
    List<Long> findBlockedIdsByBlockerId(@Param("blockerId") Long blockerId);

    @Query("SELECT ab.blocker.id FROM AccountBlock ab WHERE ab.blocked.id = :blockedId")
    List<Long> findBlockerIdsByBlockedId(@Param("blockedId") Long blockedId);

    @Query("SELECT CASE WHEN COUNT(ab) > 0 THEN true ELSE false END " +
            "FROM AccountBlock ab " +
            "WHERE (ab.blocker.id = :accountAId AND ab.blocked.id = :accountBId) " +
            "OR (ab.blocker.id = :accountBId AND ab.blocked.id = :accountAId)")
    boolean existsBetweenAccounts(@Param("accountAId") Long accountAId,
                                  @Param("accountBId") Long accountBId);

    @Query("SELECT DISTINCT CASE " +
            "WHEN ab.blocker.id = :accountId THEN ab.blocked.id " +
            "ELSE ab.blocker.id END " +
            "FROM AccountBlock ab " +
            "WHERE (ab.blocker.id = :accountId AND ab.blocked.id IN :candidateIds) " +
            "OR (ab.blocked.id = :accountId AND ab.blocker.id IN :candidateIds)")
    List<Long> findRestrictedCounterpartyIds(@Param("accountId") Long accountId,
                                             @Param("candidateIds") Collection<Long> candidateIds);
}
