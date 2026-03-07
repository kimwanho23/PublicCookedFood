package kwh.PublicCookedFood.account.controller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import kwh.PublicCookedFood.common.web.SafeRedirectSupport;
import kwh.PublicCookedFood.config.oauth2.LoginAccount;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.facade.AccountBlockFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/u/blocks")
@Hidden
public class AccountBlockController {

    private final AccountBlockFacade accountBlockFacade;

    @PostMapping("/{targetAccountId}")
    public String block(@LoginAccount Account account,
                        @PathVariable @Positive Long targetAccountId,
                        @RequestParam(required = false) String redirect,
                        HttpServletRequest request,
                        RedirectAttributes redirectAttributes) {
        if (account == null) {
            accountBlockFacade.auditBlockFailedUnauthenticated(targetAccountId);
            return "redirect:/u/login";
        }

        AccountBlockFacade.BlockOperationResult operationResult = accountBlockFacade.block(account.getId(), targetAccountId);
        redirectAttributes.addFlashAttribute("blockMessage", operationResult.message());
        markSkipViewIncrease(redirectAttributes);
        return resolveRedirect(redirect, request);
    }

    @PatchMapping("/{targetAccountId}")
    public String unblock(@LoginAccount Account account,
                          @PathVariable @Positive Long targetAccountId,
                          @RequestParam(required = false) String redirect,
                          HttpServletRequest request,
                          RedirectAttributes redirectAttributes) {
        if (account == null) {
            accountBlockFacade.auditUnblockFailedUnauthenticated(targetAccountId);
            return "redirect:/u/login";
        }

        AccountBlockFacade.BlockOperationResult operationResult = accountBlockFacade.unblock(account.getId(), targetAccountId);
        redirectAttributes.addFlashAttribute("blockMessage", operationResult.message());
        markSkipViewIncrease(redirectAttributes);
        return resolveRedirect(redirect, request);
    }

    private String resolveRedirect(String redirect, HttpServletRequest request) {
        String safeRedirect = SafeRedirectSupport.normalizeRelativePath(redirect);
        if (safeRedirect != null) {
            return SafeRedirectSupport.toRedirect(safeRedirect);
        }
        return SafeRedirectSupport.resolveRefererRedirect(request, "/boards");
    }

    private void markSkipViewIncrease(RedirectAttributes redirectAttributes) {
        if (redirectAttributes == null) {
            return;
        }
        redirectAttributes.addFlashAttribute("skipViewIncrease", true);
    }
}

