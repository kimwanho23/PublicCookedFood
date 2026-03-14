package kwh.PublicCookedFood.board.service;

import kwh.PublicCookedFood.storage.ImageDownloadQuery;
import kwh.PublicCookedFood.storage.ImageDownloadResource;
import kwh.PublicCookedFood.storage.ImageStorageService;
import kwh.PublicCookedFood.storage.ImageUploadCommand;
import kwh.PublicCookedFood.storage.StaleTempImageCleanupCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ImageStorageServiceImpl implements ImageStorageService {

    private final ImageTempUploadService imageTempUploadService;
    private final OriginalImageDownloadService originalImageDownloadService;
    private final ImageReferenceService imageReferenceService;

    @Override
    public String uploadTempImage(ImageUploadCommand command) {
        return imageTempUploadService.uploadTempImage(command);
    }

    @Override
    public Optional<ImageDownloadResource> findOriginalImageForDownload(ImageDownloadQuery query) {
        return originalImageDownloadService.findOriginalImageForDownload(query);
    }

    @Override
    public int deleteStaleTempImages(StaleTempImageCleanupCommand command) {
        return imageReferenceService.deleteStaleTempImages(command);
    }
}
