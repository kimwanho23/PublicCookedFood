package kwh.PublicCookedFood.storage;

import java.util.Optional;

public interface ImageStorageService {

    String uploadTempImage(ImageUploadCommand command);

    Optional<ImageDownloadResource> findOriginalImageForDownload(ImageDownloadQuery query);

    int deleteStaleTempImages(StaleTempImageCleanupCommand command);
}
