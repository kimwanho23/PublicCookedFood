package kwh.PublicCookedFood.account.service;

import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.dto.CustomAccountDetails;
import kwh.PublicCookedFood.account.error.AccountErrorCode;
import kwh.PublicCookedFood.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;


@Service
@RequiredArgsConstructor

public class AccountService implements UserDetailsService {

    private final AccountRepository accountRepository;

    @Transactional
    public Account save(Account account){
        accountRepository.findByEmail(account.getEmail()).ifPresent(existing -> {
            if (account.getId() == null || !existing.getId().equals(account.getId())) {
                throw new AppException(AccountErrorCode.ACCOUNT_EMAIL_DUPLICATED);
            }
        });
        accountRepository.findByName(account.getName()).ifPresent(existing -> {
            if (account.getId() == null || !existing.getId().equals(account.getId())) {
                throw new AppException(AccountErrorCode.ACCOUNT_NAME_DUPLICATED);
            }
        });
        try {
            return accountRepository.saveAndFlush(account);
        } catch (DataIntegrityViolationException e) {
            if (hasDuplicateEmail(account)) {
                throw new AppException(AccountErrorCode.ACCOUNT_EMAIL_DUPLICATED,
                        AccountErrorCode.ACCOUNT_EMAIL_DUPLICATED.message(),
                        e);
            }
            if (hasDuplicateName(account)) {
                throw new AppException(AccountErrorCode.ACCOUNT_NAME_DUPLICATED,
                        AccountErrorCode.ACCOUNT_NAME_DUPLICATED.message(),
                        e);
            }
            throw e;
        }
    }

    public Account findAccountByEmail(String email){
        return accountRepository.findByEmail(email).orElse(null);
    }

    @Transactional(readOnly = true)
    public Optional<Account> findById(Long accountId) {
        if (accountId == null) {
            return Optional.empty();
        }
        return accountRepository.findById(accountId);
    }

    public Optional<Account> findByPhoneNumberAndBirthDate(String phoneNumber, LocalDate birthDate) {
        if (phoneNumber == null || phoneNumber.isBlank() || birthDate == null) {
            return Optional.empty();
        }
        return accountRepository.findByPhoneNumberAndBirthDate(normalizePhoneNumber(phoneNumber), birthDate);
    }

    public Optional<Account> findByEmailAndPhoneNumberAndBirthDate(String email, String phoneNumber, LocalDate birthDate) {
        if (email == null || email.isBlank()
                || phoneNumber == null || phoneNumber.isBlank()
                || birthDate == null) {
            return Optional.empty();
        }
        return accountRepository.findByEmailAndPhoneNumberAndBirthDate(
                email.trim(),
                normalizePhoneNumber(phoneNumber),
                birthDate
        );
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
       Account account = accountRepository.findByEmail(email).orElse(null);

        if (account == null) {
            throw new UsernameNotFoundException(email);
        } else {
            return new CustomAccountDetails(account);
        }

    }

    private String normalizePhoneNumber(String rawPhoneNumber) {
        if (rawPhoneNumber == null) {
            return "";
        }
        return rawPhoneNumber.replaceAll("[^0-9]", "");
    }

    private boolean hasDuplicateEmail(Account account) {
        return accountRepository.findByEmail(account.getEmail())
                .filter(existing -> account.getId() == null || !existing.getId().equals(account.getId()))
                .isPresent();
    }

    private boolean hasDuplicateName(Account account) {
        return accountRepository.findByName(account.getName())
                .filter(existing -> account.getId() == null || !existing.getId().equals(account.getId()))
                .isPresent();
    }
}

