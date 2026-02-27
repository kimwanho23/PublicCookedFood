package kwh.PublicCookedFood.storage;

import java.nio.file.Path;

public final class StoragePathUtils {

    private StoragePathUtils() {
    }

    public static Path resolveUploadPath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            throw new IllegalArgumentException("file.dir 값이 비어 있습니다.");
        }

        String normalized = rawPath.trim();
        if (normalized.matches("^/[A-Za-z]:/.*")) {
            normalized = normalized.substring(1);
        }
        return Path.of(normalized).toAbsolutePath().normalize();
    }

    public static Path resolvePathUnderDirectory(Path baseDirectory, String filename) {
        if (baseDirectory == null || filename == null || filename.isBlank()) {
            return null;
        }

        String normalizedFilename = filename.trim();
        if (normalizedFilename.contains("/")
                || normalizedFilename.contains("\\")
                || normalizedFilename.contains("\0")) {
            return null;
        }

        Path resolved = baseDirectory.resolve(normalizedFilename).normalize();
        return resolved.startsWith(baseDirectory) ? resolved : null;
    }

    public static String extractBaseFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "";
        }

        String normalized = filename.replace('\\', '/');
        int slashIndex = normalized.lastIndexOf('/');
        String fileNameOnly = slashIndex >= 0 ? normalized.substring(slashIndex + 1) : normalized;
        int dotIndex = fileNameOnly.lastIndexOf('.');
        if (dotIndex <= 0) {
            return fileNameOnly;
        }
        return fileNameOnly.substring(0, dotIndex);
    }
}
