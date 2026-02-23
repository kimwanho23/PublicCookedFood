package kwh.PublicCookedFood.board.repository;

import kwh.PublicCookedFood.board.domain.BoardSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardSectionRepository extends JpaRepository<BoardSection, Long> {

    List<BoardSection> findAllByActiveTrueOrderByDisplayOrderAscIdAsc();

    List<BoardSection> findAllByOrderByDisplayOrderAscIdAsc();

    Optional<BoardSection> findBySectionKeyAndActiveTrue(String sectionKey);

    Optional<BoardSection> findBySectionKey(String sectionKey);

    boolean existsBySectionKey(String sectionKey);
}
