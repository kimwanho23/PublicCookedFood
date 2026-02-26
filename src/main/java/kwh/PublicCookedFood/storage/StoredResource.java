package kwh.PublicCookedFood.storage;

public record StoredResource(
        String originalFilename,
        String savedFilename,
        String resourceUrl,
        String contentType,
        long fileSize
) {
}
