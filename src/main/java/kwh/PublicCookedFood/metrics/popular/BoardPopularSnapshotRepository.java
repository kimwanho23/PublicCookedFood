package kwh.PublicCookedFood.metrics.popular;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BoardPopularSnapshotRepository extends JpaRepository<BoardPopularSnapshot, Long> {

    @Modifying
    @Query("DELETE FROM BoardPopularSnapshot s WHERE s.rankingType = :rankingType AND s.sectionKey = :sectionKey")
    void deleteBySlot(@Param("rankingType") String rankingType,
                      @Param("sectionKey") String sectionKey);

    @Query("SELECT COUNT(s) FROM BoardPopularSnapshot s " +
            "WHERE s.rankingType = :rankingType AND s.sectionKey = :sectionKey AND s.expiresAt > :now")
    long countActiveBySlot(@Param("rankingType") String rankingType,
                           @Param("sectionKey") String sectionKey,
                           @Param("now") LocalDateTime now);

    @Query("SELECT s FROM BoardPopularSnapshot s " +
            "WHERE s.rankingType = :rankingType AND s.sectionKey = :sectionKey AND s.expiresAt > :now " +
            "ORDER BY s.rankNo ASC")
    List<BoardPopularSnapshot> findActiveBySlot(@Param("rankingType") String rankingType,
                                                @Param("sectionKey") String sectionKey,
                                                @Param("now") LocalDateTime now,
                                                Pageable pageable);

    @Modifying
    @Query("DELETE FROM BoardPopularSnapshot s WHERE s.expiresAt <= :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}
