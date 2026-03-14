package kwh.PublicCookedFood.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class StoragePathUtilsUnitTest {

    @TempDir
    Path tempDir;

    @Test
    void resolveRelativePathUnderDirectory_allowsNestedImagePath() {
        Path resolved = StoragePathUtils.resolveRelativePathUnderDirectory(tempDir, "user-recipes/example.jpg");

        assertThat(resolved).isEqualTo(tempDir.resolve("user-recipes/example.jpg").normalize());
    }

    @Test
    void resolveRelativePathUnderDirectory_rejectsPathTraversal() {
        Path resolved = StoragePathUtils.resolveRelativePathUnderDirectory(tempDir, "../outside.jpg");

        assertThat(resolved).isNull();
    }

    @Test
    void resolveRelativePathUnderDirectory_rejectsAbsolutePath() {
        Path resolved = StoragePathUtils.resolveRelativePathUnderDirectory(tempDir, "/images/user-recipes/example.jpg");

        assertThat(resolved).isNull();
    }
}
