package kwh.PublicCookedFood.board.repository;

import kwh.PublicCookedFood.board.domain.Likes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LikesRepository extends JpaRepository<Likes, Long> {

    Long countByBoardId(Long boardId);

    Boolean existsByBoardIdAndUserId(Long boardId, Long userId);

    Optional<Likes> findByBoardIdAndUserId(Long boardId, Long userId);
}
