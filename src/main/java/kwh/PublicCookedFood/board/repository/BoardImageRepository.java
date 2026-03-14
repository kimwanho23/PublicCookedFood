package kwh.PublicCookedFood.board.repository;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardImage;
import kwh.PublicCookedFood.storage.Images;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardImageRepository extends JpaRepository<BoardImage, Long> {

    @Query("SELECT bi FROM BoardImage bi JOIN FETCH bi.image WHERE bi.board = :board")
    List<BoardImage> findAllByBoardWithImage(@Param("board") Board board);

    Optional<BoardImage> findByBoardAndImage(Board board, Images image);

    boolean existsByImage(Images image);
}
