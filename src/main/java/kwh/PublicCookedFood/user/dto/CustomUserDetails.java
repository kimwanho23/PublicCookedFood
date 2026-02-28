package kwh.PublicCookedFood.user.dto;

import kwh.PublicCookedFood.user.domain.Role;
import kwh.PublicCookedFood.user.domain.Users;
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

public class CustomUserDetails implements UserDetails, OAuth2User {

    private static final long serialVersionUID = 1L;

    private transient Users user;
    private final Long userId;
    private final String userName;
    private final String roleKey;
    private final String password;
    private final String email;
    private final Map<String, Object> attributes;


    //formLogin
    public CustomUserDetails(Users users) {
        this.user = users;
        this.userId = users != null ? users.getId() : null;
        this.userName = users != null ? users.getName() : null;
        this.roleKey = users != null && users.getAuthority() != null ? users.getAuthority().getKey() : null;
        this.password = users != null ? users.getPassword() : null;
        this.email = users != null ? users.getEmail() : null;
        this.attributes = Map.of();
    }

    //OAuth2Login
    public CustomUserDetails(Users users, Map<String, Object> attributes) {
        this.user = users;
        this.userId = users != null ? users.getId() : null;
        this.userName = users != null ? users.getName() : null;
        this.roleKey = users != null && users.getAuthority() != null ? users.getAuthority().getKey() : null;
        this.password = users != null ? users.getPassword() : null;
        this.email = users != null ? users.getEmail() : null;
        this.attributes = toImmutableMap(attributes);
    }

    public Users getUser() {
        return user;
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
        this.user = rebuildUserSnapshot();
    }

    private Users rebuildUserSnapshot() {
        if (email == null && userId == null) {
            return null;
        }
        Role role = resolveRole(roleKey);
        return Users.builder()
                .id(userId)
                .name(userName == null || userName.isBlank() ? email : userName)
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
