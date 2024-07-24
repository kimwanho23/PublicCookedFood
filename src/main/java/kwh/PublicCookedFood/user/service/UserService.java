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
import java.util.Optional;

@Service
@RequiredArgsConstructor

public class UserService implements UserDetailsService {

    private final UserRepository userRepository;

    @Transactional
    public Users save(Users users){
        return userRepository.save(users);
    }

    public Users findUser(String loginId){
        return userRepository.findByLoginId(loginId);
    }

    public Optional<Users> findbyId(Long id){
        return userRepository.findById(id);
    }

    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
       Users users = userRepository.findByLoginId(loginId);

        if (users == null) {
            throw new UsernameNotFoundException(loginId);
        }
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(users.getAuthority().name()));

        return new CustomUserDetails(users, authorities);

    }

    public Users getUserFromSession(HttpSession session) {
        Users user = (Users) session.getAttribute("user");
        if (user == null) {
            String name = SecurityContextHolder.getContext().getAuthentication().getName();
            user = findUser(name);
            session.setAttribute("user", user);
        }
        return user;
    }


/*    public Optional<User> login(String loginId, String password, PasswordEncoder passwordEncoder) { // PasswordEncoder 추가
        return userRepository.findByLoginId(loginId)
                .filter(user -> passwordEncoder.matches(password,  user.getPassword())); // 패스워드 매칭 방식 변경
    }*/


}
