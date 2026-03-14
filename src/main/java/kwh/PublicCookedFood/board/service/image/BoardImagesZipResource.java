package kwh.PublicCookedFood.board.service.image;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class BoardImagesZipResource {

    private final String zipFilename;
    private final List<ZipImageEntry> entries;

    public BoardImagesZipResource(String zipFilename, List<ZipImageEntry> entries) {
        this.zipFilename = zipFilename;
        this.entries = entries == null
                ? Collections.<ZipImageEntry>emptyList()
                : Collections.unmodifiableList(new ArrayList<ZipImageEntry>(entries));
    }

    public String zipFilename() {
        return zipFilename;
    }

    public List<ZipImageEntry> entries() {
        return entries;
    }

    public String getZipFilename() {
        return zipFilename;
    }

    public List<ZipImageEntry> getEntries() {
        return entries;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BoardImagesZipResource)) {
            return false;
        }
        BoardImagesZipResource that = (BoardImagesZipResource) other;
        return Objects.equals(zipFilename, that.zipFilename) && Objects.equals(entries, that.entries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(zipFilename, entries);
    }
}
