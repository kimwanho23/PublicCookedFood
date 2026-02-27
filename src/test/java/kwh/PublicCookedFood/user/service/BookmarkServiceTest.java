package kwh.PublicCookedFood.user.service;

import kwh.PublicCookedFood.food.entity.Recipe_INFO;
import kwh.PublicCookedFood.food.repository.Recipe_INFO_Repository;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.dto.request.BookmarkCreateRequest;
import kwh.PublicCookedFood.user.dto.request.UserSaveDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BookmarkServiceTest {

    @Autowired
    private Recipe_INFO_Repository recipeInfoRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private BookmarkService bookmarkService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void createUser() {
        Users user = createUser("bookmark-list-" + System.nanoTime() + "@test.com");
        for (long i = 1L; i <= 3; i++) {
            createRecipe(i);
            bookmarkService.save(BookmarkCreateRequest.builder()
                    .recipeID(i)
                    .userId(user.getId())
                    .build());
        }

        assertThat(bookmarkService.findUserBookmarks(user, null)).hasSize(3);
    }

    @Test
    void addBookmarkTest() {
        Users user = createUser("bookmark-add-" + System.nanoTime() + "@test.com");
        createRecipe(5L);

        bookmarkService.save(BookmarkCreateRequest.builder()
                .recipeID(5L)
                .userId(user.getId())
                .build());

        assertThat(bookmarkService.findUserBookmarks(user, null)).hasSize(1);
    }

    private Users createUser(String email) {
        UserSaveDto userDto = new UserSaveDto();
        userDto.setEmail(email);
        userDto.setName("북마크테스터");
        userDto.setPassword("12345678");
        return userService.save(Users.createUser(userDto, passwordEncoder));
    }

    private void createRecipe(Long recipeId) {
        recipeInfoRepository.findByRecipeID(recipeId)
                .orElseGet(() -> recipeInfoRepository.save(Recipe_INFO.builder()
                        .recipeID(recipeId)
                        .recipeNMKO("recipe-" + recipeId)
                        .build()));
    }
}
