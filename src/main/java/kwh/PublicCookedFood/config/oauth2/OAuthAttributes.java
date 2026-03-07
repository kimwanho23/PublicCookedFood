package kwh.PublicCookedFood.config.oauth2;

import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.account.domain.Gender;
import kwh.PublicCookedFood.account.domain.Account;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
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
        this.attributes = toImmutableMap(attributes);
        this.nameAttributeKey = nameAttributeKey;
        this.name = name;
        this.email = email;
        this.emailVerified = emailVerified;
        this.loginMethod = loginMethod;
    }

    public Map<String, Object> getAttributes() {
        if (attributes.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
    }

    public static OAuthAttributes of(String registrationId, String principalNameAttributeName, Map<String, Object> attributes) {
        String provider = registrationId == null ? "google" : registrationId.toLowerCase(Locale.ROOT);
        Map<String, Object> safeAttributes = attributes == null ? Map.of() : attributes;
        String nameAttributeKey = principalNameAttributeName;
        OAuthProfile profile;

        switch (provider) {
            case "naver" -> {
                nameAttributeKey = "id";
                profile = extractNaverProfile(safeAttributes);
            }
            case "kakao" -> {
                nameAttributeKey = "id";
                profile = extractKakaoProfile(safeAttributes);
            }
            default -> {
                provider = "google";
                profile = extractGoogleProfile(safeAttributes);
            }
        }

        return OAuthAttributes.builder()
                .name(profile.name())
                .email(profile.email())
                .emailVerified(profile.emailVerified())
                .loginMethod(provider)
                .attributes(profile.attributes())
                .nameAttributeKey(nameAttributeKey)
                .build();
    }

    private static OAuthProfile extractGoogleProfile(Map<String, Object> attributes) {
        return new OAuthProfile(
                asString(attributes.get("name")),
                asString(attributes.get("email")),
                asBoolean(attributes.get("email_verified")),
                attributes
        );
    }

    private static OAuthProfile extractNaverProfile(Map<String, Object> attributes) {
        Map<String, Object> response = getNestedMap(attributes, "response");
        return new OAuthProfile(
                asString(response.get("name")),
                asString(response.get("email")),
                !response.containsKey("email_verified") || asBoolean(response.get("email_verified")),
                response
        );
    }

    private static OAuthProfile extractKakaoProfile(Map<String, Object> attributes) {
        Map<String, Object> account = getNestedMap(attributes, "kakao_account");
        Map<String, Object> profile = getNestedMap(account, "profile");
        boolean isEmailValid = asBoolean(account.get("is_email_valid"));
        boolean isEmailVerified = asBoolean(account.get("is_email_verified"));

        return new OAuthProfile(
                asString(profile.get("nickname")),
                asString(account.get("email")),
                isEmailValid && isEmailVerified,
                attributes
        );
    }

    private static Map<String, Object> getNestedMap(Map<String, Object> source, String key) {
        if (source == null || key == null) {
            return Map.of();
        }
        return toStringObjectMap(source.get(key));
    }

    private static Map<String, Object> toStringObjectMap(Object value) {
        if (!(value instanceof Map<?, ?> rawMap) || rawMap.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> converted = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() instanceof String key) {
                converted.put(key, entry.getValue());
            }
        }
        if (converted.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(converted);
    }

    private static Map<String, Object> toImmutableMap(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }

    public Account toEntity() {
        return toEntity(name);
    }

    public Account toEntity(String resolvedName) {
        return Account.builder()
                .name(resolvedName)
                .email(email)
                .gender(Gender.OTHER)
                .notificationEnabled(true)
                .authority(Role.USER)
                .loginMethod(loginMethod)
                .build();
    }

    private static String asString(Object value) {
        return value instanceof String text ? text : null;
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

    private record OAuthProfile(String name,
                                String email,
                                boolean emailVerified,
                                Map<String, Object> attributes) {
    }

    public static class OAuthAttributesBuilder {
        public OAuthAttributesBuilder attributes(Map<String, Object> attributes) {
            this.attributes = toImmutableMap(attributes);
            return this;
        }
    }
}
