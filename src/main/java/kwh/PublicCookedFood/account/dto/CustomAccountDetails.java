package kwh.PublicCookedFood.account.dto;

import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.account.domain.Account;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class CustomAccountDetails implements UserDetails, OAuth2User {

    @Serial
    private static final long serialVersionUID = 1L;

    @Getter
    private transient Account account;
    private final Long accountId;
    private final String accountName;
    private final String roleKey;
    private final String password;
    private final String email;
    private final boolean notificationEnabled;
    private final String loginMethod;
    private final Map<String, Object> attributes;


    //formLogin
    public CustomAccountDetails(Account account) {
        this(account, Map.of());
    }

    //OAuth2Login
    public CustomAccountDetails(Account account, Map<String, Object> attributes) {
        Account safeAccount = requireAccount(account);
        this.account = safeAccount;
        this.accountId = Objects.requireNonNull(safeAccount.getId(), "account.id");
        this.accountName = requireNonBlank(safeAccount.getName(), "account.name");
        this.roleKey = resolveRoleKey(safeAccount);
        this.password = safeAccount.getPassword();
        this.email = requireNonBlank(safeAccount.getEmail(), "account.email");
        this.notificationEnabled = safeAccount.isNotificationEnabled();
        this.loginMethod = requireNonBlank(safeAccount.getLoginMethod(), "account.loginMethod");
        this.attributes = toImmutableMap(attributes);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
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

    @Serial
    private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
        inputStream.defaultReadObject();
        try {
            this.account = rebuildAccountSnapshot();
        } catch (RuntimeException e) {
            InvalidObjectException invalidObjectException =
                    new InvalidObjectException("Invalid serialized CustomAccountDetails");
            invalidObjectException.initCause(e);
            throw invalidObjectException;
        }
    }

    private Account rebuildAccountSnapshot() {
        Role role = resolveRole(roleKey);
        String restoredEmail = requireNonBlank(email, "email");
        return Account.builder()
                .id(Objects.requireNonNull(accountId, "accountId"))
                .name(accountName == null || accountName.isBlank() ? restoredEmail : accountName)
                .email(restoredEmail)
                .password(password)
                .authority(role)
                .notificationEnabled(notificationEnabled)
                .loginMethod(requireNonBlank(loginMethod, "loginMethod"))
                .build();
    }

    private static Role resolveRole(String key) {
        String resolvedKey = requireNonBlank(key, "roleKey");
        for (Role role : Role.values()) {
            if (resolvedKey.equals(role.getKey())) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role key: " + resolvedKey);
    }

    private static Account requireAccount(Account account) {
        return Objects.requireNonNull(account, "account");
    }

    private static String resolveRoleKey(Account account) {
        Role authority = Objects.requireNonNull(account.getAuthority(), "account.authority");
        return requireNonBlank(authority.getKey(), "account.authority.key");
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
