package kwh.PublicCookedFood.user.service;

import jakarta.servlet.http.HttpSession;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.dto.CustomUserDetails;
import kwh.PublicCookedFood.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor

public class UserService implements UserDetailsService {

    private final UserRepository userRepository;

    @Transactional
    public Users save(Users users){
        return userRepository.save(users);
    }

    public Users findUserByEmail(String email){
        return userRepository.findByEmail(email).orElse(null);
    }

    public Users findUserById(Long id){
        return userRepository.findById(id).orElse(null);
    }


    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
       Users users = userRepository.findByEmail(email).orElse(null);

        if (users == null) {
            throw new UsernameNotFoundException(email);
        } else {
            return new CustomUserDetails(users);
        }

    }
}
