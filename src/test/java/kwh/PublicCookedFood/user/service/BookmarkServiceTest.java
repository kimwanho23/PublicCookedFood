package kwh.PublicCookedFood.user.service;

import kwh.PublicCookedFood.food.service.RecipeService;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.dto.BookMarkDto;
import kwh.PublicCookedFood.user.dto.UserSaveDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;


import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BookmarkServiceTest {


    @Autowired
    private RecipeService recipeService;

    @Autowired
    private UserService userService;


    @Autowired
    private BookmarkService bookmarkService;


   @Commit
    @Test
    public void createUser() {
        for (long i = 1L; i <= 3; i++) {
            BookMarkDto bookMarkDto = BookMarkDto.builder()
                    .recipeID(recipeService.getRecipe_INFO(i).toEntity())
                    .user(userService.findUser("test"))
                    .build();
            bookmarkService.save(bookMarkDto);
        }
    }

}