package kwh.PublicCookedFood.config.oauth2;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OAuthAttributesUnitTest {

    @Test
    void of_naver_mapsNestedResponseAndFiltersNonStringKeys() {
        Map<Object, Object> response = new LinkedHashMap<>();
        response.put("name", "naver-account");
        response.put("email", "naver@test.com");
        response.put(1, "ignored");
        response.put("email_verified", "true");

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("response", response);

        OAuthAttributes actual = OAuthAttributes.of("naver", "sub", attributes);

        assertThat(actual.getName()).isEqualTo("naver-account");
        assertThat(actual.getEmail()).isEqualTo("naver@test.com");
        assertThat(actual.isEmailVerified()).isTrue();
        assertThat(actual.getLoginMethod()).isEqualTo("naver");
        assertThat(actual.getNameAttributeKey()).isEqualTo("id");
        assertThat(actual.getAttributes()).containsKeys("name", "email");
        assertThat(actual.getAttributes()).doesNotContainKey("1");
    }

    @Test
    void of_kakao_usesAccountFlagsForEmailVerification() {
        Map<String, Object> profile = Map.of("nickname", "kakao-account");
        Map<String, Object> account = new HashMap<>();
        account.put("profile", profile);
        account.put("email", "kakao@test.com");
        account.put("is_email_valid", true);
        account.put("is_email_verified", true);

        Map<String, Object> attributes = Map.of("kakao_account", account);

        OAuthAttributes actual = OAuthAttributes.of("kakao", "sub", attributes);

        assertThat(actual.getName()).isEqualTo("kakao-account");
        assertThat(actual.getEmail()).isEqualTo("kakao@test.com");
        assertThat(actual.isEmailVerified()).isTrue();
        assertThat(actual.getLoginMethod()).isEqualTo("kakao");
        assertThat(actual.getNameAttributeKey()).isEqualTo("id");
    }

    @Test
    void of_defaultsToGoogleWhenProviderIsUnknown() {
        Map<String, Object> attributes = Map.of(
                "name", "Google Account",
                "email", "google@test.com",
                "email_verified", true
        );

        OAuthAttributes actual = OAuthAttributes.of("github", "sub", attributes);

        assertThat(actual.getName()).isEqualTo("Google Account");
        assertThat(actual.getEmail()).isEqualTo("google@test.com");
        assertThat(actual.isEmailVerified()).isTrue();
        assertThat(actual.getLoginMethod()).isEqualTo("google");
        assertThat(actual.getNameAttributeKey()).isEqualTo("sub");
    }
}
