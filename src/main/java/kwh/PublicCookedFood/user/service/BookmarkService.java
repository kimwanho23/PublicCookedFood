package kwh.PublicCookedFood.user.service;

import jakarta.transaction.Transactional;
import kwh.PublicCookedFood.food.entity.Recipe_INFO;
import kwh.PublicCookedFood.food.repository.Recipe_INFO_Repository;
import kwh.PublicCookedFood.user.domain.Bookmark;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.dto.request.BookmarkCreateRequest;
import kwh.PublicCookedFood.user.repository.BookmarkRepository;
import kwh.PublicCookedFood.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final UserRepository userRepository;
    private final Recipe_INFO_Repository recipeInfoRepository;
    private final BookmarkRepository bookmarkRepository;

    public List<Bookmark> findUserBookmarks(Users user) { //나의 북마크
        return bookmarkRepository.findBookmarksByUserId(user.getId());
    }

    public boolean isBookmarked(Users user, Recipe_INFO recipeId) { //이미 북마크한 게시물인지 판단
        return bookmarkRepository.existsByUserAndRecipeID(user, recipeId);
    }

   @Transactional
   public Bookmark save(BookmarkCreateRequest bookmark){ // 북마크 저장
       Users user = userRepository.findById(bookmark.getUserId())
               .orElseThrow(() -> new IllegalArgumentException("User not found"));
       Recipe_INFO recipe = recipeInfoRepository.findByRecipeID(bookmark.getRecipeID())
               .orElseThrow(() -> new RuntimeException("Recipe not found"));
       return bookmarkRepository.findByUserAndRecipeID(user, recipe)
               .orElseGet(() -> bookmarkRepository.save(Bookmark.builder()
                       .user(user)
                       .recipeID(recipe)
                       .build()));
    }

    @Transactional
    public void delete(Users user, Recipe_INFO recipeID){ // 북마크 삭제
        bookmarkRepository.deleteByUserAndRecipeID(user, recipeID);
    }


}
