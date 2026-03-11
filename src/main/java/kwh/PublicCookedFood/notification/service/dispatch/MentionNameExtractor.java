package kwh.PublicCookedFood.notification.service.dispatch;

import kwh.PublicCookedFood.account.policy.AccountNicknamePolicy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class MentionNameExtractor {

    private static final int MAX_MENTION_USERS = 5;
    private static final String LEADING_DELIMITERS = "([{\"'“‘<,.;:!?";

    public List<String> extract(String rawText) {
        if (!StringUtils.hasText(rawText)) {
            return List.of();
        }

        Set<String> mentionNames = new LinkedHashSet<>();
        int length = rawText.length();
        for (int index = 0; index < length && mentionNames.size() < MAX_MENTION_USERS; index++) {
            if (rawText.charAt(index) != '@' || !isMentionStart(rawText, index)) {
                continue;
            }
            int mentionStart = index + 1;
            int mentionEnd = findMentionEnd(rawText, mentionStart);
            if (!isValidMentionLength(mentionStart, mentionEnd)
                    || !isValidMentionBoundary(rawText, mentionEnd)) {
                index = Math.max(index, mentionEnd - 1);
                continue;
            }
            mentionNames.add(rawText.substring(mentionStart, mentionEnd));
            index = mentionEnd - 1;
        }
        return new ArrayList<>(mentionNames);
    }

    private boolean isMentionStart(String rawText, int atIndex) {
        if (atIndex == 0) {
            return true;
        }
        char previous = rawText.charAt(atIndex - 1);
        return Character.isWhitespace(previous) || LEADING_DELIMITERS.indexOf(previous) >= 0;
    }

    private int findMentionEnd(String rawText, int startIndex) {
        int index = startIndex;
        while (index < rawText.length() && isNicknameCharacter(rawText.charAt(index))) {
            index++;
        }
        return index;
    }

    private boolean isValidMentionLength(int mentionStart, int mentionEnd) {
        int length = mentionEnd - mentionStart;
        return length >= AccountNicknamePolicy.MIN_LENGTH && length <= AccountNicknamePolicy.MAX_LENGTH;
    }

    private boolean isValidMentionBoundary(String rawText, int mentionEnd) {
        if (mentionEnd >= rawText.length()) {
            return true;
        }

        char next = rawText.charAt(mentionEnd);
        if (looksLikeDomainContinuation(rawText, mentionEnd) || looksLikePathContinuation(rawText, mentionEnd)) {
            return false;
        }
        return !isNicknameCharacter(next);
    }

    private boolean looksLikeDomainContinuation(String rawText, int mentionEnd) {
        return rawText.charAt(mentionEnd) == '.'
                && mentionEnd + 1 < rawText.length()
                && Character.isLetterOrDigit(rawText.charAt(mentionEnd + 1));
    }

    private boolean looksLikePathContinuation(String rawText, int mentionEnd) {
        return rawText.charAt(mentionEnd) == '/'
                && mentionEnd + 1 < rawText.length()
                && !Character.isWhitespace(rawText.charAt(mentionEnd + 1));
    }

    private boolean isNicknameCharacter(char value) {
        return Character.isLetterOrDigit(value) || value == '_' || value == '-';
    }
}
