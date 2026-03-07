package kwh.PublicCookedFood.food.controller;

import kwh.PublicCookedFood.food.dto.response.RecipeReviewSummaryResponse;
import kwh.PublicCookedFood.food.dto.response.recipe_info.Recipe_INFO_ResponseDto;
import kwh.PublicCookedFood.food.facade.RecipeDetailFacade;
import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.account.domain.Account;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeControllerUnitTest {

    @Mock
    private RecipeDetailFacade recipeDetailFacade;

    @InjectMocks
    private RecipeController recipeController;

    @Test
    void foodDetail_increasesViewsByDefault() {
        Model model = new ExtendedModelMap();
        when(recipeDetailFacade.loadRecipeDetail(eq(10L), isNull(), eq(true))).thenReturn(viewData(10L, 100L));

        MockHttpServletRequest request = new MockHttpServletRequest();
        String viewName = recipeController.foodDetail(10L, null, model, request);

        assertThat(viewName).isEqualTo("foods/foodDetail");
        verify(recipeDetailFacade).loadRecipeDetail(10L, null, true);
    }

    @Test
    void foodDetail_skipsViewIncreaseWhenFlagExists() {
        Model model = new ExtendedModelMap();
        when(recipeDetailFacade.loadRecipeDetail(eq(10L), isNull(), eq(false))).thenReturn(viewData(10L, 100L));

        MockHttpServletRequest request = new MockHttpServletRequest();
        FlashMap flashMap = new FlashMap();
        flashMap.put("skipViewIncrease", true);
        request.setAttribute(DispatcherServlet.INPUT_FLASH_MAP_ATTRIBUTE, flashMap);
        String viewName = recipeController.foodDetail(10L, null, model, request);

        assertThat(viewName).isEqualTo("foods/foodDetail");
        verify(recipeDetailFacade).loadRecipeDetail(10L, null, false);
    }

    @Test
    void upsertReview_marksSkipViewIncreaseOnRedirect() {
        Account account = Account.builder()
                .id(3L)
                .email("account@test.com")
                .name("tester")
                .authority(Role.USER)
                .loginMethod("Current")
                .build();
        when(recipeDetailFacade.upsertReview(10L, 3L, 5, "good"))
                .thenReturn(RecipeDetailFacade.ReviewUpsertResult.success("ok"));

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        String viewName = recipeController.upsertReview(10L, account, 5, "good", redirectAttributes);

        assertThat(viewName).isEqualTo("redirect:/recipes/10");
        assertThat(redirectAttributes.getFlashAttributes().get("skipViewIncrease")).isEqualTo(Boolean.TRUE);
    }

    private RecipeDetailFacade.RecipeDetailViewData viewData(Long recipeId, long viewCount) {
        return new RecipeDetailFacade.RecipeDetailViewData(
                false,
                List.of(),
                Recipe_INFO_ResponseDto.builder().recipeID(recipeId).recipeNMKO("name").build(),
                List.of(),
                List.of(),
                viewCount,
                new RecipeReviewSummaryResponse(0, 0),
                List.of(),
                null
        );
    }
}
