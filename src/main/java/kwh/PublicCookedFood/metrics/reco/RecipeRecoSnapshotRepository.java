package kwh.PublicCookedFood.metrics.reco;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface RecipeRecoSnapshotRepository extends JpaRepository<RecipeRecoSnapshot, Long> {

    @Modifying
    @Query("DELETE FROM RecipeRecoSnapshot s WHERE s.slotDate = :slotDate AND s.slotType = :slotType")
    void deleteBySlot(@Param("slotDate") LocalDate slotDate,
                      @Param("slotType") String slotType);

    @Query("SELECT s FROM RecipeRecoSnapshot s " +
            "WHERE s.slotDate = :slotDate AND s.slotType = :slotType AND s.expiresAt > :now " +
            "ORDER BY s.rankNo ASC")
    List<RecipeRecoSnapshot> findActiveBySlot(@Param("slotDate") LocalDate slotDate,
                                              @Param("slotType") String slotType,
                                              @Param("now") LocalDateTime now,
                                              Pageable pageable);

    @Modifying
    @Query("DELETE FROM RecipeRecoSnapshot s WHERE s.expiresAt <= :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}
