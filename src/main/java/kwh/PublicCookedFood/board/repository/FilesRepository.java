package kwh.PublicCookedFood.board.repository;

import kwh.PublicCookedFood.board.domain.Files;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FilesRepository extends JpaRepository<Files, Long> {


}
