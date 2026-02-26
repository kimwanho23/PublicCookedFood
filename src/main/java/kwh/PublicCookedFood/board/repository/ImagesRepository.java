package kwh.PublicCookedFood.board.repository;

import kwh.PublicCookedFood.board.domain.Images;
import kwh.PublicCookedFood.board.domain.Images.ImageStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ImagesRepository extends JpaRepository<Images, Long> {

    Optional<Images> findByImgUrlAndStatusIn(String imgUrl, Collection<ImageStatus> statuses);

    @Query("SELECT i FROM Images i " +
            "WHERE i.status = :status AND i.regTime < :cutoff " +
            "ORDER BY i.regTime ASC")
    List<Images> findStaleImages(@Param("status") ImageStatus status,
                                 @Param("cutoff") LocalDateTime cutoff,
                                 Pageable pageable);

    @Modifying
    @Query("DELETE FROM Images i WHERE i.id = :id AND i.status = :status")
    int deleteByIdAndStatus(@Param("id") Long id, @Param("status") ImageStatus status);
}
