package kwh.PublicCookedFood.user.service;

import kwh.PublicCookedFood.food.entity.Recipe_INFO;
import kwh.PublicCookedFood.food.repository.Recipe_INFO_Repository;
import kwh.PublicCookedFood.user.domain.Bookmark;
import kwh.PublicCookedFood.user.domain.Role;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.dto.request.BookmarkCreateRequest;
import kwh.PublicCookedFood.user.repository.BookmarkRepository;
import kwh.PublicCookedFood.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookmarkServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private Recipe_INFO_Repository recipeInfoRepository;

    @Mock
    private BookmarkRepository bookmarkRepository;

    @InjectMocks
    private BookmarkService bookmarkService;

    @Test
    void createUser() {
        Users user = createUser(1L, "bookmark-list@test.com");
        Bookmark b1 = Bookmark.of(user, createRecipe(1L, "recipe-1"));
        Bookmark b2 = Bookmark.of(user, createRecipe(2L, "recipe-2"));
        Bookmark b3 = Bookmark.of(user, createRecipe(3L, "recipe-3"));
        when(bookmarkRepository.findBookmarksByUserId(1L)).thenReturn(List.of(b1, b2, b3));

        List<Bookmark> bookmarks = bookmarkService.findUserBookmarks(user, null);

        assertThat(bookmarks).hasSize(3);
        verify(bookmarkRepository).findBookmarksByUserId(1L);
    }

    @Test
    void addBookmarkTest() {
        Users user = createUser(2L, "bookmark-add@test.com");
        Recipe_INFO recipe = createRecipe(5L, "recipe-5");
        BookmarkCreateRequest request = BookmarkCreateRequest.builder()
                .recipeID(5L)
                .userId(2L)
                .build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(recipeInfoRepository.findByRecipeID(5L)).thenReturn(Optional.of(recipe));
        when(bookmarkRepository.findByUserAndRecipeID(user, recipe)).thenReturn(Optional.empty());
        when(bookmarkRepository.saveAndFlush(any(Bookmark.class))).thenReturn(Bookmark.of(user, recipe));

        Bookmark saved = bookmarkService.save(request);

        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getRecipeID()).isSameAs(recipe);
        verify(bookmarkRepository).saveAndFlush(any(Bookmark.class));
    }

    @Test
    void addBookmarkTest_returnsExistingBookmarkWhenAlreadySaved() {
        Users user = createUser(3L, "bookmark-existing@test.com");
        Recipe_INFO recipe = createRecipe(7L, "recipe-7");
        Bookmark existing = Bookmark.of(user, recipe);
        BookmarkCreateRequest request = BookmarkCreateRequest.builder()
                .recipeID(7L)
                .userId(3L)
                .build();

        when(userRepository.findById(3L)).thenReturn(Optional.of(user));
        when(recipeInfoRepository.findByRecipeID(7L)).thenReturn(Optional.of(recipe));
        when(bookmarkRepository.findByUserAndRecipeID(user, recipe)).thenReturn(Optional.of(existing));

        Bookmark saved = bookmarkService.save(request);

        assertThat(saved).isSameAs(existing);
        verify(bookmarkRepository, never()).saveAndFlush(any(Bookmark.class));
    }

    private Users createUser(Long id, String email) {
        return Users.builder()
                .id(id)
                .email(email)
                .name("북마크테스터")
                .authority(Role.USER)
                .loginMethod("Current")
                .build();
    }

    private Recipe_INFO createRecipe(Long recipeId, String recipeName) {
        return Recipe_INFO.builder()
                .rowNUM(recipeId)
                .recipeID(recipeId)
                .recipeNMKO(recipeName)
                .build();
    }
}
