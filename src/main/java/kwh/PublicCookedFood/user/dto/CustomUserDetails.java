package kwh.PublicCookedFood.user.dto;

import kwh.PublicCookedFood.user.domain.Users;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

@Slf4j
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
        Collection<GrantedAuthority> collect = new ArrayList<>();
        collect.add((GrantedAuthority) user::getEmail);
        return collect;
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
        return null;
    }
}