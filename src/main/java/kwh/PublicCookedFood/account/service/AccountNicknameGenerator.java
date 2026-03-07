package kwh.PublicCookedFood.account.service;

import kwh.PublicCookedFood.account.policy.AccountNicknamePolicy;
import kwh.PublicCookedFood.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountNicknameGenerator {

    private static final String FALLBACK_PREFIX = "account";
    private static final int MAX_GENERATION_ATTEMPTS = 10_000;

    private final AccountRepository accountRepository;

    public String generateAvailableNickname(String preferredName, String email) {
        String baseNickname = buildBaseNickname(preferredName, email);
        if (accountRepository.findByName(baseNickname).isEmpty()) {
            return baseNickname;
        }

        for (int sequence = 2; sequence <= MAX_GENERATION_ATTEMPTS; sequence++) {
            String candidate = appendSequence(baseNickname, sequence);
            if (accountRepository.findByName(candidate).isEmpty()) {
                return candidate;
            }
        }

        throw new IllegalStateException("사용 가능한 닉네임을 생성하지 못했습니다.");
    }

    private String buildBaseNickname(String preferredName, String email) {
        String preferredCandidate = AccountNicknamePolicy.sanitizeCandidate(preferredName);
        if (isValidBaseNickname(preferredCandidate)) {
            return preferredCandidate;
        }

        String emailCandidate = AccountNicknamePolicy.sanitizeEmailLocalPart(email);
        if (isValidBaseNickname(emailCandidate)) {
            return emailCandidate;
        }

        return appendPadding(FALLBACK_PREFIX);
    }

    private boolean isValidBaseNickname(String candidate) {
        return candidate != null
                && candidate.length() >= AccountNicknamePolicy.MIN_LENGTH
                && candidate.length() <= AccountNicknamePolicy.MAX_LENGTH;
    }

    private String appendSequence(String baseNickname, int sequence) {
        String suffix = "-" + sequence;
        int maxBaseLength = AccountNicknamePolicy.MAX_LENGTH - suffix.length();
        String trimmedBase = baseNickname.length() > maxBaseLength
                ? baseNickname.substring(0, Math.max(maxBaseLength, 1))
                : baseNickname;
        if (trimmedBase.length() < AccountNicknamePolicy.MIN_LENGTH - suffix.length()) {
            trimmedBase = appendPadding(trimmedBase);
            if (trimmedBase.length() > maxBaseLength) {
                trimmedBase = trimmedBase.substring(0, maxBaseLength);
            }
        }
        return trimmedBase + suffix;
    }

    private String appendPadding(String rawBase) {
        StringBuilder builder = new StringBuilder(rawBase == null ? "" : rawBase);
        while (builder.length() < AccountNicknamePolicy.MIN_LENGTH) {
            builder.append('x');
        }
        if (builder.length() > AccountNicknamePolicy.MAX_LENGTH) {
            return builder.substring(0, AccountNicknamePolicy.MAX_LENGTH);
        }
        return builder.toString();
    }
}

