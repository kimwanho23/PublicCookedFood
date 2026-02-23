package kwh.PublicCookedFood.user.service;

import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.dto.CustomUserDetails;
import kwh.PublicCookedFood.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor

public class UserService implements UserDetailsService {

    private final UserRepository userRepository;

    @Transactional
    public Users save(Users users){
        userRepository.findByEmail(users.getEmail()).ifPresent(existing -> {
            if (users.getId() == null || !existing.getId().equals(users.getId())) {
                throw new IllegalStateException("이미 가입된 이메일입니다.");
            }
        });
        return userRepository.save(users);
    }

    public Users findUserByEmail(String email){
        return userRepository.findByEmail(email).orElse(null);
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
