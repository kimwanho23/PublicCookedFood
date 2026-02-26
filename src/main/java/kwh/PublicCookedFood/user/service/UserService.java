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

import java.util.Optional;


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

    @Transactional(readOnly = true)
    public Optional<Users> findById(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return userRepository.findById(userId);
    }

    public Optional<Users> findByNameAndPhoneNumber(String name, String phoneNumber) {
        if (name == null || name.isBlank() || phoneNumber == null || phoneNumber.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByNameAndPhoneNumber(name.trim(), normalizePhoneNumber(phoneNumber));
    }

    public Optional<Users> findByEmailAndNameAndPhoneNumber(String email, String name, String phoneNumber) {
        if (email == null || email.isBlank()
                || name == null || name.isBlank()
                || phoneNumber == null || phoneNumber.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByEmailAndNameAndPhoneNumber(
                email.trim(),
                name.trim(),
                normalizePhoneNumber(phoneNumber)
        );
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

    private String normalizePhoneNumber(String rawPhoneNumber) {
        if (rawPhoneNumber == null) {
            return "";
        }
        return rawPhoneNumber.replaceAll("[^0-9]", "");
    }
}
