package kwh.PublicCookedFood.board.service.image;

import java.nio.file.Path;
import java.util.Objects;

public final class ZipImageEntry {

    private final Path filePath;
    private final String entryName;

    public ZipImageEntry(Path filePath, String entryName) {
        this.filePath = filePath;
        this.entryName = entryName;
    }

    public Path filePath() {
        return filePath;
    }

    public String entryName() {
        return entryName;
    }

    public Path getFilePath() {
        return filePath;
    }

    public String getEntryName() {
        return entryName;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ZipImageEntry)) {
            return false;
        }
        ZipImageEntry that = (ZipImageEntry) other;
        return Objects.equals(filePath, that.filePath) && Objects.equals(entryName, that.entryName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filePath, entryName);
    }
}
