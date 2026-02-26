package kwh.PublicCookedFood.board.repository;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardSection;
import kwh.PublicCookedFood.board.domain.SoftDeleteState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

    @Modifying
    @Query("update Board p set p.views = p.views + 1 where p.id = :id and p.state = :state and p.hiddenByReport = false")
    int updateViews(@Param("id") Long id, @Param("state") SoftDeleteState state);

    @Modifying
    @Query("UPDATE Board b SET b.likeCount = (SELECT COUNT(l) FROM Likes l WHERE l.board.id = :boardId) WHERE b.id = :boardId")
    void updateLikes(@Param("boardId") Long boardId);

    @Modifying
    @Query("UPDATE Board b SET b.commentCount = :commentCount WHERE b.id = :postId and b.state = :state")
    void updateCommentCount(@Param("postId") Long postId,
                            @Param("commentCount") Long commentCount,
                            @Param("state") SoftDeleteState state);

    @Query(
            value = "SELECT b FROM Board b " +
                    "JOIN FETCH b.user " +
                    "LEFT JOIN FETCH b.section s " +
                    "WHERE b.state = :state " +
                    "AND b.hiddenByReport = false " +
                    "AND (:excludeBlocked = false OR b.user.id NOT IN :blockedUserIds) " +
                    "AND (:authorId IS NULL OR b.user.id = :authorId) " +
                    "AND (:sectionKey IS NULL OR (s IS NOT NULL AND s.sectionKey = :sectionKey)) " +
                    "AND b.title LIKE CONCAT('%', :search, '%')",
            countQuery = "SELECT COUNT(b) FROM Board b " +
                    "LEFT JOIN b.section s " +
                    "WHERE b.state = :state " +
                    "AND b.hiddenByReport = false " +
                    "AND (:excludeBlocked = false OR b.user.id NOT IN :blockedUserIds) " +
                    "AND (:authorId IS NULL OR b.user.id = :authorId) " +
                    "AND (:sectionKey IS NULL OR (s IS NOT NULL AND s.sectionKey = :sectionKey)) " +
                    "AND b.title LIKE CONCAT('%', :search, '%')"
    )
    Page<Board> findByTitleContainingAndStateWithUser(@Param("search") String search,
                                                      @Param("state") SoftDeleteState state,
                                                      @Param("excludeBlocked") boolean excludeBlocked,
                                                      @Param("blockedUserIds") Collection<Long> blockedUserIds,
                                                      @Param("authorId") Long authorId,
                                                      @Param("sectionKey") String sectionKey,
                                                      Pageable pageable);

    @Query(
            value = "SELECT b FROM Board b " +
                    "JOIN FETCH b.user " +
                    "LEFT JOIN FETCH b.section s " +
                    "WHERE b.state = :state " +
                    "AND b.hiddenByReport = false " +
                    "AND (:excludeBlocked = false OR b.user.id NOT IN :blockedUserIds) " +
                    "AND (:authorId IS NULL OR b.user.id = :authorId) " +
                    "AND (:sectionKey IS NULL OR (s IS NOT NULL AND s.sectionKey = :sectionKey))",
            countQuery = "SELECT COUNT(b) FROM Board b " +
                    "LEFT JOIN b.section s " +
                    "WHERE b.state = :state " +
                    "AND b.hiddenByReport = false " +
                    "AND (:excludeBlocked = false OR b.user.id NOT IN :blockedUserIds) " +
                    "AND (:authorId IS NULL OR b.user.id = :authorId) " +
                    "AND (:sectionKey IS NULL OR (s IS NOT NULL AND s.sectionKey = :sectionKey))"
    )
    Page<Board> findAllByStateWithUser(@Param("state") SoftDeleteState state,
                                       @Param("excludeBlocked") boolean excludeBlocked,
                                       @Param("blockedUserIds") Collection<Long> blockedUserIds,
                                       @Param("authorId") Long authorId,
                                       @Param("sectionKey") String sectionKey,
                                       Pageable pageable);

    @Query(
            value = "SELECT b FROM Board b " +
                    "JOIN FETCH b.user " +
                    "LEFT JOIN FETCH b.section s " +
                    "WHERE b.state = :state " +
                    "AND b.hiddenByReport = false " +
                    "AND (:excludeBlocked = false OR b.user.id NOT IN :blockedUserIds) " +
                    "AND (:authorId IS NULL OR b.user.id = :authorId) " +
                    "AND b.likeCount >= :threshold " +
                    "AND (:sectionKey IS NULL OR (s IS NOT NULL AND s.sectionKey = :sectionKey)) " +
                    "AND b.title LIKE CONCAT('%', :search, '%') " +
                    "ORDER BY b.likeCount DESC, b.regTime DESC",
            countQuery = "SELECT COUNT(b) FROM Board b " +
                    "LEFT JOIN b.section s " +
                    "WHERE b.state = :state " +
                    "AND b.hiddenByReport = false " +
                    "AND (:excludeBlocked = false OR b.user.id NOT IN :blockedUserIds) " +
                    "AND (:authorId IS NULL OR b.user.id = :authorId) " +
                    "AND b.likeCount >= :threshold " +
                    "AND (:sectionKey IS NULL OR (s IS NOT NULL AND s.sectionKey = :sectionKey)) " +
                    "AND b.title LIKE CONCAT('%', :search, '%')"
    )
    Page<Board> findFeaturedByTitleContainingAndStateWithUser(@Param("search") String search,
                                                              @Param("state") SoftDeleteState state,
                                                              @Param("excludeBlocked") boolean excludeBlocked,
                                                              @Param("blockedUserIds") Collection<Long> blockedUserIds,
                                                              @Param("authorId") Long authorId,
                                                              @Param("threshold") Long threshold,
                                                              @Param("sectionKey") String sectionKey,
                                                              Pageable pageable);

    @Query(
            value = "SELECT b FROM Board b " +
                    "JOIN FETCH b.user " +
                    "LEFT JOIN FETCH b.section s " +
                    "WHERE b.state = :state " +
                    "AND b.hiddenByReport = false " +
                    "AND (:excludeBlocked = false OR b.user.id NOT IN :blockedUserIds) " +
                    "AND (:authorId IS NULL OR b.user.id = :authorId) " +
                    "AND b.likeCount >= :threshold " +
                    "AND (:sectionKey IS NULL OR (s IS NOT NULL AND s.sectionKey = :sectionKey)) " +
                    "ORDER BY b.likeCount DESC, b.regTime DESC",
            countQuery = "SELECT COUNT(b) FROM Board b " +
                    "LEFT JOIN b.section s " +
                    "WHERE b.state = :state " +
                    "AND b.hiddenByReport = false " +
                    "AND (:excludeBlocked = false OR b.user.id NOT IN :blockedUserIds) " +
                    "AND (:authorId IS NULL OR b.user.id = :authorId) " +
                    "AND b.likeCount >= :threshold " +
                    "AND (:sectionKey IS NULL OR (s IS NOT NULL AND s.sectionKey = :sectionKey))"
    )
    Page<Board> findFeaturedByStateWithUser(@Param("state") SoftDeleteState state,
                                            @Param("excludeBlocked") boolean excludeBlocked,
                                            @Param("blockedUserIds") Collection<Long> blockedUserIds,
                                            @Param("authorId") Long authorId,
                                            @Param("threshold") Long threshold,
                                            @Param("sectionKey") String sectionKey,
                                            Pageable pageable);

    @Query("SELECT b FROM Board b JOIN FETCH b.user LEFT JOIN FETCH b.section WHERE b.id = :id")
    Optional<Board> findByIdWithUser(@Param("id") Long id);

    @Query("SELECT b FROM Board b JOIN FETCH b.user LEFT JOIN FETCH b.section " +
            "WHERE b.id = :id AND b.state = :state AND b.hiddenByReport = false")
    Optional<Board> findByIdWithUserAndState(@Param("id") Long id, @Param("state") SoftDeleteState state);

    boolean existsBySectionAndState(BoardSection section, SoftDeleteState state);

    long countByUserIdAndState(Long userId, SoftDeleteState state);

    long countByHiddenByReportTrue();

    @Query("SELECT b FROM Board b " +
            "JOIN FETCH b.user " +
            "LEFT JOIN FETCH b.section " +
            "WHERE b.state = :state AND b.hiddenByReport = false AND b.regTime >= :since " +
            "ORDER BY b.likeCount DESC, b.commentCount DESC, b.regTime DESC")
    List<Board> findTopByStateAndRegTimeAfterOrderByPopularity(@Param("state") SoftDeleteState state,
                                                                @Param("since") java.time.LocalDateTime since,
                                                                Pageable pageable);


    @Modifying
    @Query("update Board p set p.state = :state where p.id = :id")
    void updateState(@Param("id") Long id, @Param("state") SoftDeleteState state);

}
