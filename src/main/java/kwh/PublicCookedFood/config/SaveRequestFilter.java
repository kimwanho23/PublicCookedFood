package kwh.PublicCookedFood.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.dto.CustomAccountDetails;
import kwh.PublicCookedFood.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class SaveRequestFilter extends OncePerRequestFilter {

    public static final String CURRENT_ACCOUNT_REQUEST_ATTRIBUTE = SaveRequestFilter.class.getName() + ".currentAccount";

    private static final String HOME_PAGE_URI = "/recipes";
    private static final String LOGIN_PAGE_URI = "/u/login";
    private static final String SIGNUP_PAGE_URI = "/u/signup";
    private static final String ACCOUNT_RECOVER_URI_PATTERN = "/u/account/**";
    private static final String NOTIFICATION_STREAM_URI = "/api/notifications/stream";

    private final AccountService accountService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (isAuthenticated(authentication) && shouldResolveCurrentAccount(request)) {
            Account currentAccount = resolveCurrentAccount(authentication);
            if (currentAccount == null && extractAccountId(authentication.getPrincipal()) != null) {
                clearAuthentication(request);
                authentication = null;
            } else if (currentAccount != null) {
                request.setAttribute(CURRENT_ACCOUNT_REQUEST_ATTRIBUTE, currentAccount);
            }
        }

        if (isAuthenticated(authentication)) {
            if (new AntPathRequestMatcher(LOGIN_PAGE_URI).matches(request)
                    || new AntPathRequestMatcher(SIGNUP_PAGE_URI).matches(request)
                    || new AntPathRequestMatcher(ACCOUNT_RECOVER_URI_PATTERN).matches(request)) {
                response.sendRedirect(HOME_PAGE_URI);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private boolean shouldResolveCurrentAccount(HttpServletRequest request) {
        if (request == null) {
            return true;
        }
        return !new AntPathRequestMatcher(NOTIFICATION_STREAM_URI).matches(request);
    }

    private Account resolveCurrentAccount(Authentication authentication) {
        Object principal = authentication == null ? null : authentication.getPrincipal();
        Long accountId = extractAccountId(principal);
        if (accountId == null) {
            return null;
        }
        return accountService.findById(accountId)
                .filter(account -> matchesAuthority(principal, account))
                .orElse(null);
    }

    private Long extractAccountId(Object principal) {
        if (principal instanceof CustomAccountDetails customAccountDetails) {
            Account account = customAccountDetails.getAccount();
            return account == null ? null : account.getId();
        }
        if (principal instanceof Account account) {
            return account.getId();
        }
        return null;
    }

    private boolean matchesAuthority(Object principal, Account account) {
        String principalRoleKey = extractRoleKey(principal);
        if (principalRoleKey == null || principalRoleKey.isBlank()) {
            return true;
        }
        if (account == null || account.getAuthority() == null || account.getAuthority().getKey() == null) {
            return false;
        }
        return principalRoleKey.equals(account.getAuthority().getKey());
    }

    private String extractRoleKey(Object principal) {
        if (principal instanceof CustomAccountDetails customAccountDetails) {
            Account account = customAccountDetails.getAccount();
            return account != null && account.getAuthority() != null ? account.getAuthority().getKey() : null;
        }
        if (principal instanceof Account account) {
            return account.getAuthority() == null ? null : account.getAuthority().getKey();
        }
        return null;
    }

    private void clearAuthentication(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        if (request == null) {
            return;
        }
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        session.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        session.invalidate();
    }
}
