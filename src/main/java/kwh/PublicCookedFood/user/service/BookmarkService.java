package kwh.PublicCookedFood.user.service;

import jakarta.transaction.Transactional;
import kwh.PublicCookedFood.food.entity.Recipe_INFO;
import kwh.PublicCookedFood.user.domain.Bookmark;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.dto.BookMarkDto;
import kwh.PublicCookedFood.user.repository.BookmarkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;

    public List<BookMarkDto> findUserBookmarks(Users user) {
        return bookmarkRepository.findByUser(user)
                .stream()
                .map(Bookmark::toResponseDto)
                .toList();
    }

    public boolean isBookmarked(Users email, Recipe_INFO recipeId) {
        return bookmarkRepository.existsByUserAndRecipeID(email, recipeId);
    }

   @Transactional
    public Bookmark save(BookMarkDto bookmark){
        return bookmarkRepository.save(bookmark.toEntity());
    }

    @Transactional
    public void delete(Users email, Recipe_INFO recipeID){
        bookmarkRepository.deleteByUserAndRecipeID(email, recipeID);
    }



}
