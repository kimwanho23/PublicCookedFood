package kwh.PublicCookedFood.user.service;

import kwh.PublicCookedFood.food.service.RecipeService;
import kwh.PublicCookedFood.user.dto.BookMarkDto;
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
                    .recipeID(recipeService.getRecipe_INFO(i).getRecipeID())
                    .email(userService.findUserByEmail("test@gmail.com").getEmail())
                    .build();
            bookmarkService.save(bookMarkDto);
        }
    }

}