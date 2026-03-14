package kwh.PublicCookedFood.userrecipe.controller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.service.AccountBlockService;
import kwh.PublicCookedFood.common.Paging;
import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.config.oauth2.LoginAccount;
import kwh.PublicCookedFood.userrecipe.domain.UserRecipeComment;
import kwh.PublicCookedFood.userrecipe.domain.UserRecipeIngredientGroup;
import kwh.PublicCookedFood.userrecipe.dto.request.UserRecipeCommentCreateRequest;
import kwh.PublicCookedFood.userrecipe.dto.request.UserRecipeIngredientRequest;
import kwh.PublicCookedFood.userrecipe.dto.request.UserRecipeReviewUpsertRequest;
import kwh.PublicCookedFood.userrecipe.dto.request.UserRecipeStepRequest;
import kwh.PublicCookedFood.userrecipe.dto.request.UserRecipeWriteRequest;
import kwh.PublicCookedFood.userrecipe.dto.response.UserRecipeCommentResponse;
import kwh.PublicCookedFood.userrecipe.dto.response.UserRecipeDetailResponse;
import kwh.PublicCookedFood.userrecipe.dto.response.UserRecipeEditFormData;
import kwh.PublicCookedFood.userrecipe.dto.response.UserRecipeListItemResponse;
import kwh.PublicCookedFood.userrecipe.dto.response.UserRecipeReviewResponse;
import kwh.PublicCookedFood.userrecipe.dto.response.UserRecipeReviewSummaryResponse;
import kwh.PublicCookedFood.userrecipe.service.UserRecipeCommandService;
import kwh.PublicCookedFood.userrecipe.service.UserRecipeCommentCreateCommand;
import kwh.PublicCookedFood.userrecipe.service.UserRecipeCommentService;
import kwh.PublicCookedFood.userrecipe.service.UserRecipeQueryService;
import kwh.PublicCookedFood.userrecipe.service.UserRecipeReviewService;
import kwh.PublicCookedFood.userrecipe.service.UserRecipeWriteCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/user-recipes")
@Hidden
public class UserRecipeController {

    private static final String LOGIN_REDIRECT = "redirect:/u/login";
    private static final String LIST_VIEW = "user-recipes/index";
    private static final String FORM_VIEW = "user-recipes/form";
    private static final String DETAIL_VIEW = "user-recipes/detail";

    private final UserRecipeCommandService userRecipeCommandService;
    private final UserRecipeQueryService userRecipeQueryService;
    private final UserRecipeCommentService userRecipeCommentService;
    private final UserRecipeReviewService userRecipeReviewService;
    private final AccountBlockService accountBlockService;

    @ModelAttribute("commentForm")
    public UserRecipeCommentCreateRequest commentForm() {
        return new UserRecipeCommentCreateRequest();
    }

    @ModelAttribute("reviewForm")
    public UserRecipeReviewUpsertRequest reviewForm() {
        return new UserRecipeReviewUpsertRequest();
    }

    @GetMapping
    public String index(@PageableDefault(page = 0, size = 12, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
                        Model model) {
        Page<UserRecipeListItemResponse> recipePage = userRecipeQueryService.getRecipeList(pageable);
        Paging.addPagingAttributes(model, recipePage, pageable);
        model.addAttribute("recipePage", recipePage);
        return LIST_VIEW;
    }

    @GetMapping("/new")
    public String newForm(@LoginAccount Account account,
                          Model model) {
        String loginRedirect = redirectIfUnauthenticated(account);
        if (loginRedirect != null) {
            return loginRedirect;
        }
        UserRecipeWriteRequest form = new UserRecipeWriteRequest();
        ensureMinimumRows(form);
        model.addAttribute("recipeForm", form);
        populateFormPageModel(model, false, null);
        return FORM_VIEW;
    }

    @PostMapping
    public String create(@LoginAccount Account account,
                         @Valid @ModelAttribute("recipeForm") UserRecipeWriteRequest recipeForm,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        String loginRedirect = redirectIfUnauthenticated(account);
        if (loginRedirect != null) {
            return loginRedirect;
        }

        ensureMinimumRows(recipeForm);
        if (bindingResult.hasErrors()) {
            populateFormPageModel(model, false, null);
            return FORM_VIEW;
        }

        try {
            Long recipeId = userRecipeCommandService.createRecipe(account.getId(), toWriteCommand(recipeForm));
            redirectAttributes.addFlashAttribute("recipeMessage", "사용자 레시피가 등록되었습니다.");
            return "redirect:/user-recipes/" + recipeId;
        } catch (IllegalArgumentException e) {
            model.addAttribute("recipeErrorMessage", e.getMessage());
            populateFormPageModel(model, false, null);
            return FORM_VIEW;
        }
    }

    @GetMapping("/{id:[0-9]+}/edit")
    public String editForm(@LoginAccount Account account,
                           @PathVariable Long id,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        String loginRedirect = redirectIfUnauthenticated(account);
        if (loginRedirect != null) {
            return loginRedirect;
        }

        try {
            UserRecipeWriteRequest form = toWriteRequest(userRecipeQueryService.getRecipeEditFormData(id, account.getId()));
            ensureMinimumRows(form);
            model.addAttribute("recipeForm", form);
            populateFormPageModel(model, true, id);
            return FORM_VIEW;
        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("recipeErrorMessage", e.getMessage());
            return "redirect:/user-recipes/" + id;
        }
    }

    @PatchMapping("/{id:[0-9]+}")
    public String update(@LoginAccount Account account,
                         @PathVariable Long id,
                         @Valid @ModelAttribute("recipeForm") UserRecipeWriteRequest recipeForm,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        String loginRedirect = redirectIfUnauthenticated(account);
        if (loginRedirect != null) {
            return loginRedirect;
        }

        ensureMinimumRows(recipeForm);
        if (bindingResult.hasErrors()) {
            populateFormPageModel(model, true, id);
            return FORM_VIEW;
        }

        try {
            userRecipeCommandService.updateRecipe(id, account.getId(), toWriteCommand(recipeForm));
            redirectAttributes.addFlashAttribute("recipeMessage", "사용자 레시피가 수정되었습니다.");
            return "redirect:/user-recipes/" + id;
        } catch (RuntimeException e) {
            model.addAttribute("recipeErrorMessage", e.getMessage());
            populateFormPageModel(model, true, id);
            return FORM_VIEW;
        }
    }

    @GetMapping("/{id:[0-9]+}")
    public String detail(@PathVariable Long id,
                         @LoginAccount Account account,
                         @RequestParam(name = "commentPage", defaultValue = "0") int commentPage,
                         @RequestParam(name = "commentSize", defaultValue = "20") int commentSize,
                         Model model) {
        UserRecipeDetailResponse recipe = userRecipeQueryService.getRecipeDetail(id);
        Pageable commentPageable = resolveCommentPageable(commentPage, commentSize);
        Long viewerAccountId = account == null ? null : account.getId();
        Page<UserRecipeCommentResponse> comments =
                userRecipeCommentService.getCommentListWithReplies(id, commentPageable, viewerAccountId);
        UserRecipeReviewSummaryResponse reviewSummary = userRecipeReviewService.getSummary(id);
        List<UserRecipeReviewResponse> reviews = userRecipeReviewService.getRecentReviews(id);
        UserRecipeReviewResponse myReview = viewerAccountId == null ? null : userRecipeReviewService.getMyReview(id, viewerAccountId);
        boolean recipeInteractionBlocked = viewerAccountId != null
                && recipe.accountId() != null
                && accountBlockService.isEitherBlocked(viewerAccountId, recipe.accountId());
        boolean isRecipeOwner = viewerAccountId != null
                && recipe.accountId() != null
                && viewerAccountId.equals(recipe.accountId());
        boolean canWriteReview = viewerAccountId != null && !recipeInteractionBlocked && !isRecipeOwner;

        model.addAttribute("recipe", recipe);
        model.addAttribute("ingredientGroups", UserRecipeIngredientGroup.values());
        model.addAttribute("comments", comments);
        model.addAttribute("commentsCount", userRecipeCommentService.getCommentsCount(id, viewerAccountId));
        model.addAttribute("reviewSummary", reviewSummary);
        model.addAttribute("reviews", reviews);
        model.addAttribute("myReview", myReview);
        model.addAttribute("currentAccountId", viewerAccountId);
        model.addAttribute("recipeInteractionBlocked", recipeInteractionBlocked);
        model.addAttribute("isRecipeOwner", isRecipeOwner);
        model.addAttribute("canWriteReview", canWriteReview);
        Paging.addPagingAttributes(model, comments, commentPageable);
        return DETAIL_VIEW;
    }

    @PatchMapping("/{id:[0-9]+}/delete")
    public String deleteRecipe(@LoginAccount Account account,
                               @PathVariable Long id,
                               RedirectAttributes redirectAttributes) {
        String loginRedirect = redirectIfUnauthenticated(account);
        if (loginRedirect != null) {
            return loginRedirect;
        }

        try {
            userRecipeCommandService.deleteRecipe(id, account.getId());
            redirectAttributes.addFlashAttribute("recipeMessage", "사용자 레시피가 삭제되었습니다.");
            return "redirect:/user-recipes";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("recipeErrorMessage", e.getMessage());
            return "redirect:/user-recipes/" + id;
        }
    }

    @PostMapping("/{id:[0-9]+}/comments")
    public String addComment(@LoginAccount Account account,
                             @PathVariable Long id,
                             @RequestParam(name = "commentPage", defaultValue = "0") int commentPage,
                             @RequestParam(name = "commentSize", defaultValue = "20") int commentSize,
                             @Valid @ModelAttribute("commentForm") UserRecipeCommentCreateRequest commentForm,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes) {
        String loginRedirect = redirectIfUnauthenticated(account);
        if (loginRedirect != null) {
            return loginRedirect;
        }
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("commentError", "댓글 내용을 확인해주세요.");
            return commentPageRedirect(id, commentPage, commentSize);
        }

        try {
            userRecipeCommentService.createComment(toCommentCreateCommand(account.getId(), id, commentForm));
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("commentError", e.getMessage());
        }
        return commentPageRedirect(id, commentPage, commentSize);
    }

    @PatchMapping("/{id:[0-9]+}/comments/{commentId:[0-9]+}/delete")
    public String deleteComment(@LoginAccount Account account,
                                @PathVariable Long id,
                                @PathVariable Long commentId,
                                @RequestParam(name = "commentPage", defaultValue = "0") int commentPage,
                                @RequestParam(name = "commentSize", defaultValue = "20") int commentSize,
                                RedirectAttributes redirectAttributes) {
        String loginRedirect = redirectIfUnauthenticated(account);
        if (loginRedirect != null) {
            return loginRedirect;
        }

        UserRecipeComment comment = userRecipeCommentService.getComment(commentId);
        if (comment.getRecipe() == null || !id.equals(comment.getRecipe().getId())) {
            redirectAttributes.addFlashAttribute("commentError", "댓글 정보를 찾을 수 없습니다.");
            return commentPageRedirect(id, commentPage, commentSize);
        }
        if (comment.getAccount() == null || !account.getId().equals(comment.getAccount().getId())) {
            redirectAttributes.addFlashAttribute("commentError", "댓글 삭제 권한이 없습니다.");
            return commentPageRedirect(id, commentPage, commentSize);
        }

        userRecipeCommentService.deleteComment(commentId);
        return commentPageRedirect(id, commentPage, commentSize);
    }

    @PostMapping("/{id:[0-9]+}/reviews")
    public String upsertReview(@LoginAccount Account account,
                               @PathVariable Long id,
                               @Valid @ModelAttribute("reviewForm") UserRecipeReviewUpsertRequest reviewForm,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {
        String loginRedirect = redirectIfUnauthenticated(account);
        if (loginRedirect != null) {
            return loginRedirect;
        }
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("reviewErrorMessage", "리뷰 내용을 확인해주세요.");
            return "redirect:/user-recipes/" + id + "#recipe-reviews";
        }

        try {
            userRecipeReviewService.upsertReview(
                    id,
                    account.getId(),
                    reviewForm.getRating(),
                    reviewForm.getContents(),
                    reviewForm.getImageUrl()
            );
            redirectAttributes.addFlashAttribute("reviewMessage", "리뷰가 저장되었습니다.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("reviewErrorMessage", e.getMessage());
        }
        return "redirect:/user-recipes/" + id + "#recipe-reviews";
    }

    @PatchMapping("/{id:[0-9]+}/reviews/delete")
    public String deleteReview(@LoginAccount Account account,
                               @PathVariable Long id,
                               RedirectAttributes redirectAttributes) {
        String loginRedirect = redirectIfUnauthenticated(account);
        if (loginRedirect != null) {
            return loginRedirect;
        }

        try {
            userRecipeReviewService.deleteReview(id, account.getId());
            redirectAttributes.addFlashAttribute("reviewMessage", "리뷰가 삭제되었습니다.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("reviewErrorMessage", e.getMessage());
        }
        return "redirect:/user-recipes/" + id + "#recipe-reviews";
    }

    private void populateFormOptions(Model model) {
        model.addAttribute("ingredientGroups", UserRecipeIngredientGroup.values());
        model.addAttribute("difficultyOptions", List.of("초급", "중급", "고급"));
    }

    private void populateFormPageModel(Model model, boolean editMode, Long recipeId) {
        populateFormOptions(model);
        model.addAttribute("formMode", editMode ? "edit" : "create");
        model.addAttribute("formTitle", editMode ? "사용자 레시피 수정" : "사용자 레시피 작성");
        model.addAttribute("formIntro", editMode
                ? "기존 레시피 내용을 수정합니다. 재료와 조리 단계도 현재 구조 그대로 편집할 수 있습니다."
                : "대표 이미지, 재료, 조리 단계를 구조화해서 등록합니다. 조리 단계는 최대 15단계까지 작성할 수 있습니다.");
        model.addAttribute("formSubmitLabel", editMode ? "레시피 수정" : "레시피 등록");
        model.addAttribute("formAction", editMode ? "/user-recipes/" + recipeId : "/user-recipes");
        model.addAttribute("recipeId", recipeId);
    }

    private void ensureMinimumRows(UserRecipeWriteRequest form) {
        if (form.getIngredients() == null) {
            form.setIngredients(new java.util.ArrayList<>());
        }
        if (form.getIngredients().isEmpty()) {
            form.getIngredients().add(new UserRecipeIngredientRequest());
        }
        if (form.getSteps() == null) {
            form.setSteps(new java.util.ArrayList<>());
        }
        if (form.getSteps().isEmpty()) {
            UserRecipeStepRequest step = new UserRecipeStepRequest();
            step.setStepNo(1);
            form.getSteps().add(step);
        }
    }

    private UserRecipeWriteRequest toWriteRequest(UserRecipeEditFormData formData) {
        UserRecipeWriteRequest request = new UserRecipeWriteRequest();
        request.setTitle(formData.title());
        request.setSummary(formData.summary());
        request.setThumbnailUrl(formData.thumbnailUrl());
        request.setCookingTime(formData.cookingTime());
        request.setServings(formData.servings());
        request.setDifficulty(formData.difficulty());
        request.setIngredients(formData.ingredients().stream()
                .map(ingredient -> {
                    UserRecipeIngredientRequest row = new UserRecipeIngredientRequest();
                    row.setIngredientGroup(ingredient.ingredientGroup());
                    row.setIngredientName(ingredient.ingredientName());
                    row.setAmountText(ingredient.amountText());
                    row.setSortOrder(ingredient.sortOrder());
                    return row;
                })
                .toList());
        request.setSteps(formData.steps().stream()
                .map(step -> {
                    UserRecipeStepRequest row = new UserRecipeStepRequest();
                    row.setStepNo(step.stepNo());
                    row.setContents(step.contents());
                    row.setTip(step.tip());
                    row.setImageUrl(step.imageUrl());
                    return row;
                })
                .toList());
        return request;
    }

    private UserRecipeWriteCommand toWriteCommand(UserRecipeWriteRequest form) {
        return new UserRecipeWriteCommand(
                form.getTitle(),
                form.getSummary(),
                form.getThumbnailUrl(),
                form.getCookingTime(),
                form.getServings(),
                form.getDifficulty(),
                form.getIngredients().stream()
                        .map(ingredient -> new UserRecipeWriteCommand.IngredientItem(
                                ingredient.getIngredientGroup(),
                                ingredient.getIngredientName(),
                                ingredient.getAmountText()
                        ))
                        .toList(),
                form.getSteps().stream()
                        .map(step -> new UserRecipeWriteCommand.StepItem(
                                step.getContents(),
                                step.getTip(),
                                step.getImageUrl()
                        ))
                        .toList()
        );
    }

    private UserRecipeCommentCreateCommand toCommentCreateCommand(Long accountId,
                                                                  Long recipeId,
                                                                  UserRecipeCommentCreateRequest commentForm) {
        return UserRecipeCommentCreateCommand.of(
                accountId,
                recipeId,
                commentForm.getContents(),
                commentForm.getParentId()
        );
    }

    private Pageable resolveCommentPageable(int commentPage, int commentSize) {
        int normalizedPage = Math.max(commentPage, 0);
        int normalizedSize = Math.max(1, Math.min(commentSize, 100));
        return PageRequest.of(normalizedPage, normalizedSize, Sort.by(Sort.Direction.ASC, "regTime"));
    }

    private String redirectIfUnauthenticated(Account account) {
        return account == null ? LOGIN_REDIRECT : null;
    }

    private String commentPageRedirect(Long recipeId, int commentPage, int commentSize) {
        Pageable pageable = resolveCommentPageable(commentPage, commentSize);
        return "redirect:/user-recipes/" + recipeId
                + "?commentPage=" + pageable.getPageNumber()
                + "&commentSize=" + pageable.getPageSize()
                + "#recipe-comments";
    }
}
