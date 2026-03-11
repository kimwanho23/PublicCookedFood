package kwh.PublicCookedFood.common.util;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ImmutableCollections {

    private ImmutableCollections() {
    }

    public static <T> List<T> immutableList(Collection<? extends T> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return List.copyOf(source);
    }

    public static <T> Set<T> immutableSet(Collection<? extends T> source) {
        if (source == null || source.isEmpty()) {
            return Set.of();
        }
        return Collections.unmodifiableSet(new LinkedHashSet<>(source));
    }

    public static <K, V> Map<K, V> immutableMap(Map<? extends K, ? extends V> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }
}
