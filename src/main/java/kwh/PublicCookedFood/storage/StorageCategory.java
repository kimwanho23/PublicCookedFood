package kwh.PublicCookedFood.storage;

import lombok.Getter;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

@Getter
public enum StorageCategory {
    IMAGE("images", "/images/"),
    USER_RECIPE_IMAGE("images/user-recipes", "/images/user-recipes/"),
    ATTACHMENT("files", "/files/");

    private final String directoryName;
    private final String urlPrefix;

    StorageCategory(String directoryName, String urlPrefix) {
        this.directoryName = directoryName;
        this.urlPrefix = urlPrefix;
    }

    public static Optional<StorageCategory> resolveByUrl(String resourceUrl) {
        if (resourceUrl == null || resourceUrl.isBlank()) {
            return Optional.empty();
        }
        String trimmed = resourceUrl.trim();
        return Arrays.stream(values())
                .filter(category -> trimmed.startsWith(category.urlPrefix))
                .max(Comparator.comparingInt(category -> category.urlPrefix.length()));
    }

}
