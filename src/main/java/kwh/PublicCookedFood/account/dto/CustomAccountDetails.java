package kwh.PublicCookedFood.account.dto;

import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.account.domain.Account;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CustomAccountDetails implements UserDetails, OAuth2User {

    private static final long serialVersionUID = 1L;

    private transient Account account;
    private final Long accountId;
    private final String accountName;
    private final String roleKey;
    private final String password;
    private final String email;
    private final Map<String, Object> attributes;


    //formLogin
    public CustomAccountDetails(Account account) {
        this.account = account;
        this.accountId = account != null ? account.getId() : null;
        this.accountName = account != null ? account.getName() : null;
        this.roleKey = account != null && account.getAuthority() != null ? account.getAuthority().getKey() : null;
        this.password = account != null ? account.getPassword() : null;
        this.email = account != null ? account.getEmail() : null;
        this.attributes = Map.of();
    }

    //OAuth2Login
    public CustomAccountDetails(Account account, Map<String, Object> attributes) {
        this.account = account;
        this.accountId = account != null ? account.getId() : null;
        this.accountName = account != null ? account.getName() : null;
        this.roleKey = account != null && account.getAuthority() != null ? account.getAuthority().getKey() : null;
        this.password = account != null ? account.getPassword() : null;
        this.email = account != null ? account.getEmail() : null;
        this.attributes = toImmutableMap(attributes);
    }

    public Account getAccount() {
        return account;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes.isEmpty() ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (roleKey == null || roleKey.isBlank()) {
            return Collections.emptyList();
        }
        return List.of(() -> roleKey);
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getName() {
        return email;
    }

    private static Map<String, Object> toImmutableMap(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }

    private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
        inputStream.defaultReadObject();
        this.account = rebuildAccountSnapshot();
    }

    private Account rebuildAccountSnapshot() {
        if (email == null && accountId == null) {
            return null;
        }
        Role role = resolveRole(roleKey);
        return Account.builder()
                .id(accountId)
                .name(accountName == null || accountName.isBlank() ? email : accountName)
                .email(email)
                .password(password)
                .authority(role)
                .notificationEnabled(true)
                .loginMethod("Current")
                .build();
    }

    private static Role resolveRole(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        for (Role role : Role.values()) {
            if (key.equals(role.getKey())) {
                return role;
            }
        }
        return null;
    }
}
