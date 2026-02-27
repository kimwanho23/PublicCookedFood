package kwh.PublicCookedFood.user.controller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kwh.PublicCookedFood.common.Paging;
import kwh.PublicCookedFood.config.oauth2.LoginUser;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.dto.request.LoginDto;
import kwh.PublicCookedFood.user.dto.request.UserSaveDto;
import kwh.PublicCookedFood.user.dto.request.UserUpdateDto;
import kwh.PublicCookedFood.user.facade.UserAuthFacade;
import kwh.PublicCookedFood.user.facade.UserProfileFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/u")
@Hidden
public class UserController {

    private final UserAuthFacade userAuthFacade;
    private final UserProfileFacade userProfileFacade;

    @GetMapping(value = "/signup")
    public String saveForm(@ModelAttribute("userSaveDto") UserSaveDto userSaveDto,
                           HttpServletRequest request) {
        userAuthFacade.rememberPreviousPage(request, "/u/signup");
        return "/user/signUpForm";
    }

    @PostMapping(value = "/signup")
    public String save(@Valid UserSaveDto userSaveDto,
                       BindingResult bindingResult,
                       Model model) {
        if (bindingResult.hasErrors()) {
            return "/user/signUpForm";
        }

        UserAuthFacade.SignupResult signupResult = userAuthFacade.signup(userSaveDto);
        if (!signupResult.success()) {
            model.addAttribute("errorMessage", signupResult.errorMessage());
            return "/user/signUpForm";
        }
        return "redirect:/recipes";
    }

    @GetMapping(value = "/profile")
    public String profileForm(@LoginUser Users user,
                              @ModelAttribute("userUpdateDto") UserUpdateDto userUpdateDto,
                              Model model) {
        if (user == null) {
            return "redirect:/u/login";
        }
        userProfileFacade.populateUserUpdateDto(userUpdateDto, user);
        model.addAttribute("user", user);
        return "/user/profile";
    }

    @GetMapping("/settings")
    public String settingsForm(@LoginUser Users user, Model model) {
        if (user == null) {
            return "redirect:/u/login";
        }
        model.addAttribute("user", user);
        model.addAttribute("blockedUsers", userProfileFacade.getBlockedUsers(user.getId()));
        return "/user/settings";
    }

    @GetMapping({"/{userId:[0-9]+}", "/{userId:[0-9]+}/{view:comments|scraps}"})
    public String otherProfile(@PathVariable Long userId,
                               @PathVariable(name = "view", required = false) String view,
                               @LoginUser Users loginUser,
                               @PageableDefault(page = 0, size = 10, sort = "regTime", direction = Sort.Direction.DESC) Pageable pageable,
                               Model model) {
        UserProfileFacade.OtherProfileViewData viewData =
                userProfileFacade.loadOtherProfile(userId, view, loginUser, pageable);

        model.addAttribute("profileView", viewData.profileView());
        Paging.addPagingAttributes(model, viewData.activePage(), pageable);
        switch (viewData.profileView()) {
            case "comments" -> model.addAttribute("commentPage", viewData.commentPage());
            case "scraps" -> model.addAttribute("scrapPage", viewData.scrapPage());
            default -> model.addAttribute("boardPage", viewData.boardPage());
        }

        model.addAttribute("profileUser", viewData.profileUser());
        model.addAttribute("currentUserId", viewData.currentUserId());
        model.addAttribute("myBlockedProfileUser", viewData.myBlockedProfileUser());
        return "/user/otherProfile";
    }

    @PutMapping("/profile")
    public String update(@LoginUser Users user,
                         @Valid UserUpdateDto userUpdateDto,
                         BindingResult bindingResult,
                         Model model) {
        if (user == null) {
            userAuthFacade.markProfileUpdateFailedUnauthenticated();
            return "redirect:/u/login";
        }
        model.addAttribute("user", user);

        if (bindingResult.hasErrors()) {
            userAuthFacade.markProfileUpdateFailedValidation(user.getId());
            return "/user/profile";
        }

        UserProfileFacade.ProfileUpdateResult updateResult = userProfileFacade.updateProfile(user, userUpdateDto);
        if (!updateResult.success()) {
            model.addAttribute("errorMessage", updateResult.errorMessage());
            return "/user/profile";
        }

        Users savedUser = updateResult.savedUser();
        model.addAttribute("user", savedUser);
        userAuthFacade.refreshAuthenticationPrincipal(savedUser);
        return "redirect:/u/profile";
    }

    @GetMapping("/login")
    public String login(@ModelAttribute("loginDto") LoginDto loginDto,
                        @RequestParam(required = false) String error,
                        @RequestParam(required = false) Long retryAfter,
                        @RequestParam(required = false) Boolean oauthError,
                        HttpServletRequest request,
                        Model model) {
        userAuthFacade.rememberPreviousPage(request, "/u/login");
        userAuthFacade.populateLoginErrorMessage(model, error, retryAfter, oauthError);
        return "user/loginForm";
    }

    @GetMapping(value = "/login/error")
    public String loginError(Model model) {
        userAuthFacade.applyDefaultLoginError(model);
        return "/user/loginForm";
    }
}
