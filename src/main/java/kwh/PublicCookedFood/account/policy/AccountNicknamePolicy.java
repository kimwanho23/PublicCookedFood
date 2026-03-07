package kwh.PublicCookedFood.account.policy;

import java.util.Locale;

public final class AccountNicknamePolicy {

    public static final int MIN_LENGTH = 2;
    public static final int MAX_LENGTH = 20;
    public static final String REGEX_BODY = "[\\p{L}\\p{N}_-]{2,20}";
    public static final String REGEX = "^" + REGEX_BODY + "$";

    private AccountNicknamePolicy() {
    }

    public static String normalizeRequiredNickname(String rawNickname) {
        if (rawNickname == null) {
            throw new IllegalArgumentException("닉네임을 입력해주세요.");
        }

        String normalized = rawNickname.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("닉네임을 입력해주세요.");
        }
        if (normalized.length() < MIN_LENGTH || normalized.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("닉네임은 2자 이상 20자 이하여야 합니다.");
        }
        if (!normalized.matches(REGEX)) {
            throw new IllegalArgumentException("닉네임은 한글, 영문, 숫자, 밑줄(_), 하이픈(-)만 사용할 수 있습니다.");
        }
        return normalized;
    }

    public static String sanitizeCandidate(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        rawValue.trim().codePoints().forEach(codePoint -> {
            if (Character.isLetterOrDigit(codePoint) || codePoint == '_' || codePoint == '-') {
                builder.appendCodePoint(codePoint);
            }
        });

        if (builder.length() > MAX_LENGTH) {
            return builder.substring(0, MAX_LENGTH);
        }
        return builder.toString();
    }

    public static String sanitizeEmailLocalPart(String email) {
        if (email == null || email.isBlank()) {
            return "";
        }
        int atIndex = email.indexOf('@');
        String localPart = atIndex > 0 ? email.substring(0, atIndex) : email;
        return sanitizeCandidate(localPart.toLowerCase(Locale.ROOT));
    }
}
