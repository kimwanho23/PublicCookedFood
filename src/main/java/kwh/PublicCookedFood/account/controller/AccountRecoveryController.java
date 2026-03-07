package kwh.PublicCookedFood.account.controller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import kwh.PublicCookedFood.account.facade.AccountRecoveryFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/u")
@Hidden
public class AccountRecoveryController {

    private static final String ACCOUNT_RECOVER_VIEW = "/account/accountRecoverForm";
    private static final String TAB_FIND_EMAIL = "find-email";
    private static final String TAB_RESET_PASSWORD = "reset-password";

    private final AccountRecoveryFacade accountRecoveryFacade;

    @GetMapping("/account/recover")
    public String accountRecoverForm(@RequestParam(required = false) String tab, Model model) {
        model.addAttribute("activeTab", accountRecoveryFacade.resolveActiveTab(tab));
        populateRecoveryDefaults(model);
        return ACCOUNT_RECOVER_VIEW;
    }

    @PostMapping("/account/find-email")
    public String findAccountEmail(@RequestParam String birthDate,
                                   @RequestParam String phoneNumber,
                                   HttpServletRequest request,
                                   Model model) {
        model.addAttribute("activeTab", TAB_FIND_EMAIL);
        model.addAttribute("findBirthDate", birthDate);
        model.addAttribute("findPhoneNumber", phoneNumber);
        populateRecoveryDefaults(model);

        AccountRecoveryFacade.RecoveryOperationResult result =
                accountRecoveryFacade.findAccountEmail(request, birthDate, phoneNumber);
        applyRecoveryResult(model, result);
        if (result.foundEmail() != null) {
            model.addAttribute("foundEmail", result.foundEmail());
        }
        return ACCOUNT_RECOVER_VIEW;
    }

    @PostMapping("/account/reset-password")
    public String resetAccountPassword(@RequestParam String email,
                                       @RequestParam String birthDate,
                                       @RequestParam String phoneNumber,
                                       @RequestParam String verificationCode,
                                       @RequestParam String newPassword,
                                       @RequestParam String confirmPassword,
                                       HttpServletRequest request,
                                       Model model) {
        model.addAttribute("activeTab", TAB_RESET_PASSWORD);
        model.addAttribute("resetEmail", email);
        model.addAttribute("resetBirthDate", birthDate);
        model.addAttribute("resetPhoneNumber", phoneNumber);
        model.addAttribute("resetVerificationCode", verificationCode);
        populateRecoveryDefaults(model);

        AccountRecoveryFacade.RecoveryOperationResult result =
                accountRecoveryFacade.resetAccountPassword(
                        request,
                        email,
                        birthDate,
                        phoneNumber,
                        verificationCode,
                        newPassword,
                        confirmPassword
                );
        applyRecoveryResult(model, result);
        return ACCOUNT_RECOVER_VIEW;
    }

    @PostMapping("/account/reset-password/code")
    public String requestPasswordResetCode(@RequestParam String email,
                                           @RequestParam String birthDate,
                                           @RequestParam String phoneNumber,
                                           HttpServletRequest request,
                                           Model model) {
        model.addAttribute("activeTab", TAB_RESET_PASSWORD);
        model.addAttribute("resetEmail", email);
        model.addAttribute("resetBirthDate", birthDate);
        model.addAttribute("resetPhoneNumber", phoneNumber);
        populateRecoveryDefaults(model);

        AccountRecoveryFacade.RecoveryOperationResult result =
                accountRecoveryFacade.requestPasswordResetCode(request, email, birthDate, phoneNumber);
        applyRecoveryResult(model, result);
        return ACCOUNT_RECOVER_VIEW;
    }

    private void applyRecoveryResult(Model model,
                                     AccountRecoveryFacade.RecoveryOperationResult result) {
        if (result.success()) {
            model.addAttribute("recoverSuccess", result.message());
        } else {
            model.addAttribute("recoverError", result.message());
        }
    }

    private void populateRecoveryDefaults(Model model) {
        if (!model.containsAttribute("findBirthDate")) {
            model.addAttribute("findBirthDate", "");
        }
        if (!model.containsAttribute("findPhoneNumber")) {
            model.addAttribute("findPhoneNumber", "");
        }
        if (!model.containsAttribute("resetEmail")) {
            model.addAttribute("resetEmail", "");
        }
        if (!model.containsAttribute("resetBirthDate")) {
            model.addAttribute("resetBirthDate", "");
        }
        if (!model.containsAttribute("resetPhoneNumber")) {
            model.addAttribute("resetPhoneNumber", "");
        }
        if (!model.containsAttribute("resetVerificationCode")) {
            model.addAttribute("resetVerificationCode", "");
        }
    }
}
