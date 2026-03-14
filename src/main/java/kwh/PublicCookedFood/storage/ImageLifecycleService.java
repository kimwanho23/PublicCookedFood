package kwh.PublicCookedFood.storage;

public interface ImageLifecycleService {

    void attachImagesIfPresent(ImageUrls imageUrls);

    void cleanupImagesByUrlIfUnlinked(ImageUrls imageUrls);
}
