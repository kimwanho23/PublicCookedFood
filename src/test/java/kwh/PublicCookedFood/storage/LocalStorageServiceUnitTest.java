package kwh.PublicCookedFood.storage;

import kwh.PublicCookedFood.config.properties.ImageProcessingProperties;
import kwh.PublicCookedFood.config.properties.StorageProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LocalStorageServiceUnitTest {

    @TempDir
    Path tempDir;

    @Test
    void store_resizesUserRecipeImagesWhenImageProcessingIsEnabled() throws IOException {
        LocalStorageService localStorageService = new LocalStorageService(
                new StorageProperties(tempDir.toString()),
                new ImageProcessingProperties(true, 120, 120, 0.85f)
        );
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large-user-recipe.jpg",
                "image/jpeg",
                largeJpegImageBytes(480, 240)
        );

        StoredResource storedResource = localStorageService.store(file, StorageCategory.USER_RECIPE_IMAGE);

        Path storedPath = tempDir.resolve("images/user-recipes").resolve(storedResource.savedFilename());
        BufferedImage storedImage = ImageIO.read(Files.newInputStream(storedPath));

        assertThat(storedResource.resourceUrl()).startsWith("/images/user-recipes/");
        assertThat(storedImage).isNotNull();
        assertThat(storedImage.getWidth()).isLessThanOrEqualTo(120);
        assertThat(storedImage.getHeight()).isLessThanOrEqualTo(120);
    }

    private byte[] largeJpegImageBytes(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.ORANGE);
            graphics.fillRect(0, 0, width, height);
        } finally {
            graphics.dispose();
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", outputStream);
        return outputStream.toByteArray();
    }
}
