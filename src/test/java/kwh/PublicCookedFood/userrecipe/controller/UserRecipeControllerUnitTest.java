package kwh.PublicCookedFood.userrecipe.controller;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.account.service.AccountBlockService;
import kwh.PublicCookedFood.config.oauth2.LoginAccount;
import kwh.PublicCookedFood.userrecipe.domain.UserRecipe;
import kwh.PublicCookedFood.userrecipe.domain.UserRecipeComment;
import kwh.PublicCookedFood.userrecipe.service.UserRecipeCommandService;
import kwh.PublicCookedFood.userrecipe.service.UserRecipeCommentService;
import kwh.PublicCookedFood.userrecipe.service.UserRecipeQueryService;
import kwh.PublicCookedFood.userrecipe.service.UserRecipeReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserRecipeControllerUnitTest {

    @Mock
    private UserRecipeCommandService userRecipeCommandService;

    @Mock
    private UserRecipeQueryService userRecipeQueryService;

    @Mock
    private UserRecipeCommentService userRecipeCommentService;

    @Mock
    private UserRecipeReviewService userRecipeReviewService;

    @Mock
    private AccountBlockService accountBlockService;

    private MockMvc mockMvc;
    private Account loginAccount;

    @BeforeEach
    void setUp() {
        UserRecipeController controller = new UserRecipeController(
                userRecipeCommandService,
                userRecipeQueryService,
                userRecipeCommentService,
                userRecipeReviewService,
                accountBlockService
        );
        loginAccount = account(7L, "tester");
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new FixedLoginAccountArgumentResolver(loginAccount))
                .build();
    }

    @Test
    void deleteComment_acceptsPatchRequest() throws Exception {
        UserRecipe recipe = recipe(10L, loginAccount);
        UserRecipeComment comment = UserRecipeComment.builder()
                .id(20L)
                .account(loginAccount)
                .recipe(recipe)
                .contents("comment")
                .build();
        when(userRecipeCommentService.getComment(20L)).thenReturn(comment);

        mockMvc.perform(patch("/user-recipes/10/comments/20/delete")
                        .param("commentPage", "1")
                        .param("commentSize", "30"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user-recipes/10?commentPage=1&commentSize=30#recipe-comments"));

        verify(userRecipeCommentService).deleteComment(20L);
    }

    @Test
    void deleteReview_acceptsPatchRequest() throws Exception {
        mockMvc.perform(patch("/user-recipes/10/reviews/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user-recipes/10#recipe-reviews"));

        verify(userRecipeReviewService).deleteReview(10L, 7L);
    }

    @Test
    void update_acceptsPatchRequest() throws Exception {
        mockMvc.perform(patch("/user-recipes/10")
                        .param("title", "수정 레시피")
                        .param("thumbnailUrl", "/images/user-recipes/thumbnail.jpg")
                        .param("ingredients[0].ingredientGroup", "MAIN")
                        .param("ingredients[0].ingredientName", "달걀")
                        .param("ingredients[0].amountText", "2개")
                        .param("steps[0].stepNo", "1")
                        .param("steps[0].contents", "섞는다"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user-recipes/10"));

        verify(userRecipeCommandService).updateRecipe(org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.eq(7L), any());
    }

    private Account account(Long id, String name) {
        return Account.builder()
                .id(id)
                .email(name + "@test.com")
                .name(name)
                .authority(Role.USER)
                .loginMethod("Current")
                .build();
    }

    private UserRecipe recipe(Long id, Account account) {
        UserRecipe recipe = mock(UserRecipe.class);
        when(recipe.getId()).thenReturn(id);
        return recipe;
    }

    private record FixedLoginAccountArgumentResolver(Account account) implements HandlerMethodArgumentResolver {

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(LoginAccount.class)
                    && Account.class.equals(parameter.getParameterType());
        }

        @Override
        public Object resolveArgument(MethodParameter parameter,
                                      ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest,
                                      WebDataBinderFactory binderFactory) {
            return account;
        }
    }
}
