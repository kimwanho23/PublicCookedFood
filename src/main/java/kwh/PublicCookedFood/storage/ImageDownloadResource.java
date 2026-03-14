package kwh.PublicCookedFood.storage;

import java.nio.file.Path;

public record ImageDownloadResource(Path filePath,
                                    String downloadFilename,
                                    String contentType,
                                    long fileSize) {
}
