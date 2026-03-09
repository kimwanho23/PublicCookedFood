package kwh.PublicCookedFood.account.service;

import kwh.PublicCookedFood.config.properties.InitialAdminProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InitialAdminInitializer implements ApplicationRunner {

    private final AccountService accountService;
    private final InitialAdminProperties initialAdminProperties;

    @Override
    public void run(ApplicationArguments args) {
        String initialAdminEmail = normalize(initialAdminProperties.initialAdminEmail());
        if (initialAdminEmail == null) {
            return;
        }

        boolean promoted = accountService.promoteToInitialAdminIfPresent(initialAdminEmail);
        if (promoted) {
            log.info("action=account.initial_admin result=promoted");
            return;
        }

        log.info("action=account.initial_admin result=not_found_or_already_admin");
    }

    private String normalize(String rawValue) {
        if (rawValue == null) {
            return null;
        }
        String trimmed = rawValue.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
