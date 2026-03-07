package kwh.PublicCookedFood.config.oauth2;

import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.account.error.AccountErrorCode;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.dto.CustomAccountDetails;
import kwh.PublicCookedFood.account.repository.AccountRepository;
import kwh.PublicCookedFood.account.service.AccountNicknameGenerator;
import kwh.PublicCookedFood.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CustomOAuth2AccountService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    private static final int MAX_NICKNAME_RETRY_COUNT = 5;

    private final AccountRepository accountRepository;
    private final AccountNicknameGenerator accountNicknameGenerator;
    private final AccountService accountService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User>
                delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        String nameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        OAuthAttributes attributes = OAuthAttributes.
                of(registrationId, nameAttributeName, oAuth2User.getAttributes());

        Account account = saveOrUpdate(attributes);

        return new CustomAccountDetails(account, oAuth2User.getAttributes());
    }


    Account saveOrUpdate(OAuthAttributes attributes) {
        if (attributes.getEmail() == null || attributes.getEmail().isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_token"),
                    "OAuth provider did not return a usable email.");
        }
        if (!attributes.isEmailVerified()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_token"),
                    "OAuth provider email is not verified.");
        }

        return accountRepository.findByEmail(attributes.getEmail())
                .orElseGet(() -> {
                    AppException lastDuplicateNameException = null;
                    for (int attempt = 0; attempt < MAX_NICKNAME_RETRY_COUNT; attempt++) {
                        String resolvedNickname =
                                accountNicknameGenerator.generateAvailableNickname(attributes.getName(), attributes.getEmail());
                        try {
                            return accountService.save(attributes.toEntity(resolvedNickname));
                        } catch (AppException e) {
                            if (e.getErrorCode() == AccountErrorCode.ACCOUNT_NAME_DUPLICATED) {
                                lastDuplicateNameException = e;
                                continue;
                            }
                            if (e.getErrorCode() == AccountErrorCode.ACCOUNT_EMAIL_DUPLICATED) {
                                return accountRepository.findByEmail(attributes.getEmail()).orElseThrow(() -> e);
                            }
                            throw e;
                        }
                    }
                    throw new IllegalStateException("OAuth 사용자 닉네임을 생성하지 못했습니다.", lastDuplicateNameException);
                });
    }
}
