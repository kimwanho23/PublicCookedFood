package kwh.PublicCookedFood.account.controller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kwh.PublicCookedFood.common.Paging;
import kwh.PublicCookedFood.config.oauth2.LoginAccount;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.dto.request.LoginDto;
import kwh.PublicCookedFood.account.dto.request.AccountSaveDto;
import kwh.PublicCookedFood.account.dto.request.AccountUpdateDto;
import kwh.PublicCookedFood.account.facade.AccountAuthFacade;
import kwh.PublicCookedFood.account.facade.AccountProfileFacade;
import kwh.PublicCookedFood.storage.StorageException;
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
public class AccountController {

    private final AccountAuthFacade accountAuthFacade;
    private final AccountProfileFacade accountProfileFacade;

    @GetMapping(value = "/signup")
    public String saveForm(@ModelAttribute("accountSaveDto") AccountSaveDto accountSaveDto,
                           HttpServletRequest request) {
        accountAuthFacade.rememberPreviousPage(request, "/u/signup");
        return "account/signUpForm";
    }

    @PostMapping(value = "/signup")
    public String save(@Valid AccountSaveDto accountSaveDto,
                       BindingResult bindingResult,
                       Model model) {
        if (bindingResult.hasErrors()) {
            return "account/signUpForm";
        }

        AccountAuthFacade.SignupResult signupResult = accountAuthFacade.signup(accountSaveDto);
        if (!signupResult.success()) {
            model.addAttribute("errorMessage", signupResult.errorMessage());
            return "account/signUpForm";
        }
        return "redirect:/recipes";
    }

    @GetMapping(value = "/profile")
    public String profileForm(@LoginAccount Account account,
                              @ModelAttribute("accountUpdateDto") AccountUpdateDto accountUpdateDto,
                              Model model) {
        if (account == null) {
            return "redirect:/u/login";
        }
        accountProfileFacade.populateAccountUpdateDto(accountUpdateDto, account);
        model.addAttribute("account", account);
        return "account/profile";
    }

    @GetMapping("/settings")
    public String settingsForm(@LoginAccount Account account, Model model) {
        if (account == null) {
            return "redirect:/u/login";
        }
        model.addAttribute("account", account);
        model.addAttribute("blockedAccounts", accountProfileFacade.getBlockedAccounts(account.getId()));
        return "account/settings";
    }

    @GetMapping({"/{accountId:[0-9]+}", "/{accountId:[0-9]+}/{view:comments|scraps}"})
    public String otherProfile(@PathVariable Long accountId,
                               @PathVariable(name = "view", required = false) String view,
                               @LoginAccount Account loginAccount,
                               @PageableDefault(page = 0, size = 10, sort = "regTime", direction = Sort.Direction.DESC) Pageable pageable,
                               Model model) {
        AccountProfileFacade.OtherProfileViewData viewData =
                accountProfileFacade.loadOtherProfile(accountId, view, loginAccount, pageable);

        model.addAttribute("profileView", viewData.profileView());
        Paging.addPagingAttributes(model, viewData.activePage(), pageable);
        switch (viewData.profileView()) {
            case "comments" -> model.addAttribute("commentPage", viewData.commentPage());
            case "scraps" -> model.addAttribute("scrapPage", viewData.scrapPage());
            default -> model.addAttribute("boardPage", viewData.boardPage());
        }

        model.addAttribute("profileAccount", viewData.profileAccount());
        model.addAttribute("currentAccountId", viewData.currentAccountId());
        model.addAttribute("myBlockedProfileAccount", viewData.myBlockedProfileAccount());
        return "account/otherProfile";
    }

    @PutMapping("/profile")
    public String update(@LoginAccount Account account,
                         @Valid AccountUpdateDto accountUpdateDto,
                         BindingResult bindingResult,
                         Model model) {
        if (account == null) {
            accountAuthFacade.markProfileUpdateFailedUnauthenticated();
            return "redirect:/u/login";
        }
        model.addAttribute("account", account);

        if (bindingResult.hasErrors()) {
            accountAuthFacade.markProfileUpdateFailedValidation(account.getId());
            return "account/profile";
        }

        AccountProfileFacade.ProfileUpdateResult updateResult;
        try {
            updateResult = accountProfileFacade.updateProfile(account, accountUpdateDto);
        } catch (StorageException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "account/profile";
        }
        if (!updateResult.success()) {
            model.addAttribute("errorMessage", updateResult.errorMessage());
            return "account/profile";
        }

        Account savedAccount = updateResult.savedAccount();
        model.addAttribute("account", savedAccount);
        accountAuthFacade.refreshAuthenticationPrincipal(savedAccount);
        return "redirect:/u/profile";
    }

    @GetMapping("/login")
    public String login(@ModelAttribute("loginDto") LoginDto loginDto,
                        @RequestParam(required = false) String error,
                        @RequestParam(required = false) Long retryAfter,
                        @RequestParam(required = false) Boolean oauthError,
                        HttpServletRequest request,
                        Model model) {
        accountAuthFacade.rememberPreviousPage(request, "/u/login");
        accountAuthFacade.populateLoginErrorMessage(model, error, retryAfter, oauthError);
        return "account/loginForm";
    }

}


