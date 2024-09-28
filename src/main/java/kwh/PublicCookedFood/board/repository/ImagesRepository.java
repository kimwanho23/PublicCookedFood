package kwh.PublicCookedFood.board.repository;

import kwh.PublicCookedFood.board.domain.Images;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImagesRepository extends JpaRepository<Images, Long> {


}
