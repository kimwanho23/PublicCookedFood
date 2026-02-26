package kwh.PublicCookedFood.board.repository;

import kwh.PublicCookedFood.board.domain.BoardPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BoardPolicyRepository extends JpaRepository<BoardPolicy, Long> {

    Optional<BoardPolicy> findTopByOrderByIdAsc();
}
