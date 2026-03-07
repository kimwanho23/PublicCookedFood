package kwh.PublicCookedFood.account.facade;

import jakarta.servlet.http.HttpServletRequest;
import kwh.PublicCookedFood.board.service.ImageService;
import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.account.audit.AccountAuditPublisher;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.dto.CustomAccountDetails;
import kwh.PublicCookedFood.account.dto.request.AccountSaveDto;
import kwh.PublicCookedFood.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;

@Service
@RequiredArgsConstructor
public class AccountAuthFacade {

    private static final String DEFAULT_LOGIN_ERROR_MESSAGE = "아이디 또는 비밀번호를 확인해주세요.";
    private static final String OAUTH_LOGIN_ERROR_MESSAGE = "소셜 로그인에 실패했습니다. OAuth 클라이언트 설정(redirect URI, client id/secret)을 확인해주세요.";

    private final AccountService accountService;
    private final AccountAuditPublisher accountAuditPublisher;
    private final PasswordEncoder passwordEncoder;
    private final ImageService imageService;

    public void rememberPreviousPage(HttpServletRequest request, String currentPath) {
        if (request == null) {
            return;
        }
        String referer = request.getHeader("Referer");
        if (referer != null && (currentPath == null || !referer.contains(currentPath))) {
            request.getSession().setAttribute("prevPage", referer);
        }
    }

    @Transactional
    public SignupResult signup(AccountSaveDto accountSaveDto) {
        try {
            Account account = Account.createAccount(accountSaveDto, passwordEncoder);
            Account savedAccount = accountService.save(account);
            imageService.attachProfileImageIfPresent(savedAccount.getProfileImageUrl());
            accountAuditPublisher.accountSignup(savedAccount.getId(), savedAccount.getEmail());
            return SignupResult.succeeded();
        } catch (AppException e) {
            return SignupResult.failed(e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public void markProfileUpdateFailedUnauthenticated() {
        accountAuditPublisher.accountProfileUpdateFailedUnauthenticated();
    }

    @Transactional(readOnly = true)
    public void markProfileUpdateFailedValidation(Long accountId) {
        accountAuditPublisher.accountProfileUpdateFailedValidation(accountId);
    }

    public void populateLoginErrorMessage(Model model,
                                          String error,
                                          Long retryAfter,
                                          Boolean oauthError) {
        if (error != null) {
            if ("locked".equalsIgnoreCase(error)) {
                long seconds = retryAfter == null ? 0L : retryAfter;
                model.addAttribute("loginErrorMsg",
                        seconds > 0
                                ? "로그인 시도가 너무 많습니다. 약 " + seconds + "초 후 다시 시도해주세요."
                                : "로그인 시도가 너무 많습니다. 잠시 후 다시 시도해주세요.");
            } else {
                model.addAttribute("loginErrorMsg", DEFAULT_LOGIN_ERROR_MESSAGE);
            }
        }
        if (Boolean.TRUE.equals(oauthError)) {
            model.addAttribute("loginErrorMsg", OAUTH_LOGIN_ERROR_MESSAGE);
        }
    }

    public void refreshAuthenticationPrincipal(Account savedAccount) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomAccountDetails currentPrincipal)) {
            return;
        }

        CustomAccountDetails updatedPrincipal = currentPrincipal.getAttributes() == null
                ? new CustomAccountDetails(savedAccount)
                : new CustomAccountDetails(savedAccount, currentPrincipal.getAttributes());

        UsernamePasswordAuthenticationToken updatedAuthentication =
                new UsernamePasswordAuthenticationToken(
                        updatedPrincipal,
                        authentication.getCredentials(),
                        updatedPrincipal.getAuthorities());
        updatedAuthentication.setDetails(authentication.getDetails());
        SecurityContextHolder.getContext().setAuthentication(updatedAuthentication);
    }

    public record SignupResult(boolean success,
                               String errorMessage) {

        public static SignupResult succeeded() {
            return new SignupResult(true, null);
        }

        public static SignupResult failed(String errorMessage) {
            return new SignupResult(false, errorMessage);
        }
    }
}

