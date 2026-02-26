package kwh.PublicCookedFood.config.oauth2;

import kwh.PublicCookedFood.user.domain.Role;
import kwh.PublicCookedFood.user.domain.Gender;
import kwh.PublicCookedFood.user.domain.Users;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.Map;

@Getter
@ToString
public class OAuthAttributes {
    private final Map<String, Object> attributes;
    private final String nameAttributeKey;
    private final String name;
    private final String email;
    private final boolean emailVerified;
    private final String loginMethod;

    @Builder
    public OAuthAttributes(Map<String, Object> attributes, String nameAttributeKey, String name, String email,
                           boolean emailVerified, String loginMethod) {
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
        this.name = name;
        this.email = email;
        this.emailVerified = emailVerified;
        this.loginMethod = loginMethod;
    }

    public static OAuthAttributes of(String registrationId, String userNameAttributeName, Map<String, Object> attributes) {
        if (registrationId.equals("naver")) {
            return ofNaver("id", attributes);
        }
        if (registrationId.equals("kakao")) {
            return ofKakao("id", attributes);
        }
        return ofGoogle(userNameAttributeName, attributes);
    }

    // OAuth2User에서 반환하는 사용자 정보는 Map이기 때문에 값 하나하나를 변환해야한다.
    private static OAuthAttributes ofGoogle(String userNameAttributeName, Map<String, Object> attributes) {
        return OAuthAttributes.builder()
                .name((String) attributes.get("name"))
                .email((String) attributes.get("email"))
                .emailVerified(asBoolean(attributes.get("email_verified")))
                .loginMethod(("google"))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    private static OAuthAttributes ofNaver(String userNameAttributeName, Map<String, Object> attributes) {
        Map<String, Object> response = attributes.get("response") instanceof Map<?, ?> rawResponse
                ? (Map<String, Object>) rawResponse
                : Map.of();
        return OAuthAttributes.builder()
                .name((String) response.get("name"))
                .email((String) response.get("email"))
                .emailVerified(!response.containsKey("email_verified") || asBoolean(response.get("email_verified")))
                .loginMethod("naver")
                .attributes(response)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    private static OAuthAttributes ofKakao(String userNameAttributeName, Map<String, Object> attributes) {
        Map<String, Object> account = attributes.get("kakao_account") instanceof Map<?, ?> rawAccount
                ? (Map<String, Object>) rawAccount
                : Map.of();
        Map<String, Object> profile = account.get("profile") instanceof Map<?, ?> rawProfile
                ? (Map<String, Object>) rawProfile
                : Map.of();
        boolean isEmailValid = asBoolean(account.get("is_email_valid"));
        boolean isEmailVerified = asBoolean(account.get("is_email_verified"));

        return OAuthAttributes.builder()
                .name((String) profile.get("nickname"))
                .email((String) account.get("email"))
                .emailVerified(isEmailValid && isEmailVerified)
                .loginMethod("kakao")
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }


    public Users toEntity() {
        return Users.builder()
                .name(name)
                .email(email)
                .gender(Gender.OTHER)
                .notificationEnabled(true)
                .authority(Role.USER)
                .loginMethod(loginMethod)
                .build();
    }

    private static boolean asBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String text) {
            return Boolean.parseBoolean(text);
        }
        return false;
    }
}
