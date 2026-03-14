package kwh.PublicCookedFood.storage;

import org.springframework.web.multipart.MultipartFile;

public record ImageUploadCommand(MultipartFile file, StorageCategory storageCategory) {

    public ImageUploadCommand {
        storageCategory = storageCategory == null ? StorageCategory.IMAGE : storageCategory;
    }

    public static ImageUploadCommand of(MultipartFile file) {
        return new ImageUploadCommand(file, StorageCategory.IMAGE);
    }

    public static ImageUploadCommand of(MultipartFile file, StorageCategory storageCategory) {
        return new ImageUploadCommand(file, storageCategory);
    }
}
