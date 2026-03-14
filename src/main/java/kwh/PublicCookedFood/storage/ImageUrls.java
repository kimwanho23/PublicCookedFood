package kwh.PublicCookedFood.storage;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public record ImageUrls(Set<String> values) {

    public ImageUrls {
        values = values == null ? Set.of() : Set.copyOf(values);
    }

    public static ImageUrls of(Collection<String> rawValues) {
        if (rawValues == null || rawValues.isEmpty()) {
            return new ImageUrls(Set.of());
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String rawValue : rawValues) {
            if (rawValue == null) {
                continue;
            }
            String trimmed = rawValue.trim();
            if (!trimmed.isEmpty()) {
                normalized.add(trimmed);
            }
        }
        return new ImageUrls(normalized);
    }

    public static ImageUrls single(String rawValue) {
        return of(rawValue == null ? Set.of() : Set.of(rawValue));
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }
}
