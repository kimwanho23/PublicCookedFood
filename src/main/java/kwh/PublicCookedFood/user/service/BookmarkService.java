package kwh.PublicCookedFood.user.service;

import org.springframework.transaction.annotation.Transactional;
import kwh.PublicCookedFood.food.entity.Recipe_INFO;
import kwh.PublicCookedFood.food.repository.Recipe_INFO_Repository;
import kwh.PublicCookedFood.user.domain.Bookmark;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.dto.request.BookmarkCreateRequest;
import kwh.PublicCookedFood.user.repository.BookmarkRepository;
import kwh.PublicCookedFood.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final UserRepository userRepository;
    private final Recipe_INFO_Repository recipeInfoRepository;
    private final BookmarkRepository bookmarkRepository;

    public List<Bookmark> findUserBookmarks(Users user, String search) {
        String normalizedSearch = normalizeQueryText(search);

        return bookmarkRepository.findBookmarksByUserId(user.getId()).stream()
                .filter(bookmark -> normalizedSearch == null || containsRecipeText(bookmark, normalizedSearch))
                .toList();
    }

    public boolean isBookmarked(Users user, Recipe_INFO recipeId) { //이미 북마크한 게시물인지 판단
        return bookmarkRepository.existsByUserAndRecipeID(user, recipeId);
    }

   @Transactional
   public Bookmark save(BookmarkCreateRequest bookmark){ // 북마크 저장
       Users user = userRepository.findById(bookmark.getUserId())
               .orElseThrow(() -> new IllegalArgumentException("User not found"));
       Recipe_INFO recipe = recipeInfoRepository.findByRecipeID(bookmark.getRecipeID())
               .orElseThrow(() -> new IllegalArgumentException("Recipe not found"));
       try {
           return bookmarkRepository.findByUserAndRecipeID(user, recipe)
                   .map(existing -> existing)
                   .orElseGet(() -> bookmarkRepository.save(Bookmark.of(user, recipe)));
       } catch (DataIntegrityViolationException e) {
           // 동시에 중복 요청이 들어온 경우 unique 제약 충돌 후 기존 레코드 재조회
           return bookmarkRepository.findByUserAndRecipeID(user, recipe)
                   .orElseThrow(() -> e);
       }
    }

    @Transactional
    public void delete(Users user, Recipe_INFO recipeID){ // 북마크 삭제
        bookmarkRepository.deleteByUserAndRecipeID(user, recipeID);
    }

    private String normalizeQueryText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean containsRecipeText(Bookmark bookmark, String keyword) {
        if (bookmark == null || bookmark.getRecipeID() == null) {
            return false;
        }
        String normalizedKeyword = keyword.toLowerCase(Locale.ROOT);
        String recipeName = bookmark.getRecipeID().getRecipeNMKO();
        String summary = bookmark.getRecipeID().getSumry();
        return (recipeName != null && recipeName.toLowerCase(Locale.ROOT).contains(normalizedKeyword))
                || (summary != null && summary.toLowerCase(Locale.ROOT).contains(normalizedKeyword));
    }

}
