package kwh.PublicCookedFood.storage;

import lombok.Getter;

@Getter
public enum StorageCategory {
    IMAGE("images", "/images/"),
    ATTACHMENT("files", "/files/");

    private final String directoryName;
    private final String urlPrefix;

    StorageCategory(String directoryName, String urlPrefix) {
        this.directoryName = directoryName;
        this.urlPrefix = urlPrefix;
    }

}
