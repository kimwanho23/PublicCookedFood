package kwh.PublicCookedFood.board.service;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class BoardReportTextSanitizer {

    private static final int MAX_DETAILS_LENGTH = 500;
    private static final int MAX_PROCESS_NOTE_LENGTH = 500;

    public Optional<String> sanitizeDetails(String details) {
        return sanitize(details, MAX_DETAILS_LENGTH);
    }

    public Optional<String> sanitizeProcessNote(String processNote) {
        return sanitize(processNote, MAX_PROCESS_NOTE_LENGTH);
    }

    private Optional<String> sanitize(String rawText, int maxLength) {
        if (rawText == null || rawText.isBlank()) {
            return Optional.empty();
        }
        String sanitized = Jsoup.clean(rawText, Safelist.none()).trim().replaceAll("\\s+", " ");
        if (sanitized.isBlank()) {
            return Optional.empty();
        }
        if (sanitized.length() <= maxLength) {
            return Optional.of(sanitized);
        }
        return Optional.of(sanitized.substring(0, maxLength));
    }
}
