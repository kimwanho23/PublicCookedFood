package kwh.PublicCookedFood.user.service;

import jakarta.transaction.Transactional;
import kwh.PublicCookedFood.food.entity.Recipe_INFO;
import kwh.PublicCookedFood.food.repository.Recipe_INFO_Repository;
import kwh.PublicCookedFood.user.domain.Bookmark;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.dto.BookMarkDto;
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

    public List<BookMarkDto> findUserBookmarks(Users user) { //나의 북마크
        return bookmarkRepository.findByUser(user)
                .stream()
                .map(Bookmark::toResponseDto)
                .toList();
    }

    public boolean isBookmarked(Users email, Recipe_INFO recipeId) { //이미 북마크한 게시물인지 판단
        return bookmarkRepository.existsByUserAndRecipeID(email, recipeId);
    }

   @Transactional
    public Bookmark save(BookMarkDto bookmark){ // 북마크 저장
       Users user = userRepository.findByEmail(bookmark.getEmail()).orElseThrow();
       Recipe_INFO recipe = recipeInfoRepository.findById(bookmark.getRecipeID())
               .orElseThrow(() -> new RuntimeException("Recipe not found"));
        return bookmarkRepository.save(bookmark.toEntity(user, recipe));
    }

    @Transactional
    public void delete(Users email, Recipe_INFO recipeID){ // 북마크 삭제
        bookmarkRepository.deleteByUserAndRecipeID(email, recipeID);
    }



}
