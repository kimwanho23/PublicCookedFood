package kwh.PublicCookedFood.account.dto;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.domain.Role;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomAccountDetailsUnitTest {

    @Test
    void constructor_rejectsNullAccount() {
        assertThatThrownBy(() -> new CustomAccountDetails(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("account");
    }

    @Test
    void oAuthConstructor_keepsImmutableAttributeSnapshot() {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("provider", "google");

        CustomAccountDetails details = new CustomAccountDetails(account(1L), attributes);
        attributes.put("provider", "changed");

        assertThat(details.getAttributes()).containsEntry("provider", "google");
        assertThatThrownBy(() -> details.getAttributes().put("new", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void getAuthorities_returnsStoredRoleKey() {
        CustomAccountDetails details = new CustomAccountDetails(account(2L));

        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(Role.USER.getKey());
    }

    @Test
    void serialization_rebuildsTransientAccountSnapshot() throws Exception {
        CustomAccountDetails original = new CustomAccountDetails(account(7L));

        CustomAccountDetails restored = roundTrip(original);

        assertThat(restored.getAccount()).isNotNull();
        assertThat(restored.getAccount().getId()).isEqualTo(7L);
        assertThat(restored.getAccount().getEmail()).isEqualTo("user7@test.com");
        assertThat(restored.getAccount().getName()).isEqualTo("user-7");
        assertThat(restored.getAccount().getAuthority()).isEqualTo(Role.USER);
        assertThat(restored.getAttributes()).isEmpty();
    }

    private CustomAccountDetails roundTrip(CustomAccountDetails source) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream)) {
            objectOutputStream.writeObject(source);
        }

        try (ObjectInputStream objectInputStream =
                     new ObjectInputStream(new ByteArrayInputStream(outputStream.toByteArray()))) {
            return (CustomAccountDetails) objectInputStream.readObject();
        }
    }

    private Account account(Long id) {
        return Account.builder()
                .id(id)
                .email("user" + id + "@test.com")
                .password("encoded-password")
                .name("user-" + id)
                .authority(Role.USER)
                .notificationEnabled(true)
                .loginMethod("Current")
                .build();
    }
}
