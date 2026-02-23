package kwh.PublicCookedFood.user.dto;

import kwh.PublicCookedFood.user.domain.Users;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Getter
public class CustomUserDetails implements UserDetails, OAuth2User {

    private final Users user;
    private Map<String, Object> attributes;


    //formLogin
    public CustomUserDetails(Users users) {
        this.user = users;
    }

    //OAuth2Login
    public CustomUserDetails(Users users, Map<String, Object> attributes) {
        this.user = users;
        this.attributes = attributes;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (user.getAuthority() == null) {
            return Collections.emptyList();
        }
        return List.of(() -> user.getAuthority().getKey());
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public String getName() {
        return user.getEmail();
    }
}
