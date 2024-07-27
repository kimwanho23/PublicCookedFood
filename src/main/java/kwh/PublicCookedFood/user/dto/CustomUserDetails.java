package kwh.PublicCookedFood.user.dto;

import kwh.PublicCookedFood.user.domain.Users;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

@Slf4j
@Getter
public class CustomUserDetails extends User implements UserDetails {

    private final String name;

    public CustomUserDetails(Users users, List<GrantedAuthority> authorities) {
        super(users.getEmail(),users.getPassword(), authorities);
        this.name = users.getName();
    }

}
