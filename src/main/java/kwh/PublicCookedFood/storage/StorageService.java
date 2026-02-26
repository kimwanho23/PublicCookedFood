package kwh.PublicCookedFood.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    StoredResource store(MultipartFile file, StorageCategory category);

    void delete(StorageCategory category, String savedFilename);
}
