package kwh.PublicCookedFood.config.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.dto.CustomAccountDetails;
import kwh.PublicCookedFood.account.service.AccountService;
import kwh.PublicCookedFood.config.SaveRequestFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor
public class LoginAccountArgumentResolver implements HandlerMethodArgumentResolver {

    private final AccountService accountService;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        boolean isLoginUserAnnotation = parameter.getParameterAnnotation(LoginAccount.class) != null;
        boolean isUserClass = Account.class.equals(parameter.getParameterType());
        return isLoginUserAnnotation && isUserClass;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomAccountDetails customAccountDetails) {
            return resolveCurrentAccount(customAccountDetails.getAccount(), webRequest);
        }
        if (principal instanceof Account account) {
            return resolveCurrentAccount(account, webRequest);
        }
        return null;
    }

    private Account resolveCurrentAccount(Account account, NativeWebRequest webRequest) {
        Account requestScopedAccount = resolveRequestScopedAccount(webRequest);
        if (requestScopedAccount != null) {
            return requestScopedAccount;
        }
        if (isNotificationStreamRequest(webRequest)) {
            return account;
        }
        if (account == null || account.getId() == null) {
            clearAuthentication(webRequest);
            return null;
        }
        return accountService.findById(account.getId())
                .orElseGet(() -> {
                    clearAuthentication(webRequest);
                    return null;
                });
    }

    private Account resolveRequestScopedAccount(NativeWebRequest webRequest) {
        if (webRequest == null) {
            return null;
        }
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            return null;
        }
        Object cachedAccount = request.getAttribute(SaveRequestFilter.CURRENT_ACCOUNT_REQUEST_ATTRIBUTE);
        return cachedAccount instanceof Account account ? account : null;
    }

    private boolean isNotificationStreamRequest(NativeWebRequest webRequest) {
        if (webRequest == null) {
            return false;
        }
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        return SaveRequestFilter.isNotificationStreamRequest(request);
    }

    private void clearAuthentication(NativeWebRequest webRequest) {
        SecurityContextHolder.clearContext();
        if (webRequest == null) {
            return;
        }
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            return;
        }
        jakarta.servlet.http.HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        session.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
    }
}
