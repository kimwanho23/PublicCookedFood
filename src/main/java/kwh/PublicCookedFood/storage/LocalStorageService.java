package kwh.PublicCookedFood.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class LocalStorageService implements StorageService {

    private static final Set<String> RESIZABLE_IMAGE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png");
    private static final String JPEG_EXTENSION = ".jpg";
    private static final String PNG_EXTENSION = ".png";
    private static final String ORIGINAL_IMAGE_DIRECTORY = "original";
    private static final String ORIGINAL_IMAGE_SUFFIX = "_orig";

    private final Path rootPath;
    private final boolean imageProcessingEnabled;
    private final int maxImageWidth;
    private final int maxImageHeight;
    private final float jpegQuality;

    public LocalStorageService(
            @Value("${file.dir}") String fileDir,
            @Value("${app.image.processing.enabled:true}") boolean imageProcessingEnabled,
            @Value("${app.image.processing.max-width:1600}") int maxImageWidth,
            @Value("${app.image.processing.max-height:1600}") int maxImageHeight,
            @Value("${app.image.processing.jpeg-quality:0.85}") float jpegQuality
    ) {
        this.rootPath = resolveUploadPath(fileDir);
        this.imageProcessingEnabled = imageProcessingEnabled;
        this.maxImageWidth = Math.max(1, maxImageWidth);
        this.maxImageHeight = Math.max(1, maxImageHeight);
        this.jpegQuality = clampJpegQuality(jpegQuality);
    }

    @Override
    public StoredResource store(MultipartFile file, StorageCategory category) {
        if (file == null || file.isEmpty()) {
            throw new StorageException("저장할 파일이 비어 있습니다.");
        }

        String originalFilename = file.getOriginalFilename();
        String originalExtension = extractFileExtension(originalFilename);
        OptimizedImage optimizedImage = optimizeImageIfPossible(file, category, originalExtension);

        String fileExtension = optimizedImage != null ? optimizedImage.fileExtension() : originalExtension;
        String savedFilename = UUID.randomUUID() + fileExtension;

        Path categoryDir = rootPath.resolve(category.getDirectoryName()).normalize();
        Path targetPath = resolvePathUnderDirectory(categoryDir, savedFilename);
        if (targetPath == null) {
            throw new StorageException("저장 파일 경로를 확인할 수 없습니다.");
        }

        try {
            Files.createDirectories(categoryDir);
            if (optimizedImage != null && category == StorageCategory.IMAGE) {
                storeOriginalImageCopy(file, categoryDir, savedFilename, originalExtension);
            }
            if (optimizedImage != null) {
                Files.write(targetPath, optimizedImage.bytes());
            } else {
                file.transferTo(targetPath);
            }
        } catch (IOException e) {
            throw new StorageException("파일 저장에 실패했습니다.", e);
        }

        String contentType = optimizedImage != null
                ? optimizedImage.contentType()
                : (file.getContentType() == null ? "" : file.getContentType());
        long fileSize = optimizedImage != null ? optimizedImage.bytes().length : file.getSize();
        String resourceUrl = category.getUrlPrefix() + savedFilename;

        return new StoredResource(originalFilename, savedFilename, resourceUrl, contentType, fileSize);
    }

    @Override
    public void delete(StorageCategory category, String savedFilename) {
        if (savedFilename == null || savedFilename.isBlank()) {
            return;
        }

        Path categoryDir = rootPath.resolve(category.getDirectoryName()).normalize();
        Path targetPath = resolvePathUnderDirectory(categoryDir, savedFilename);
        if (targetPath == null) {
            log.warn("허용되지 않은 파일명으로 삭제 요청이 들어왔습니다. filename={}", savedFilename);
            return;
        }
        if (category == StorageCategory.IMAGE && !Files.exists(targetPath)) {
            Path legacyPath = resolvePathUnderDirectory(rootPath, savedFilename); // 기존 이미지 저장 경로 호환
            if (legacyPath != null) {
                targetPath = legacyPath;
            }
        }
        try {
            Files.deleteIfExists(targetPath);
            if (category == StorageCategory.IMAGE) {
                deleteOriginalImageCopies(categoryDir, savedFilename);
            }
        } catch (IOException e) {
            log.warn("파일 삭제에 실패했습니다. path={}", targetPath, e);
        }
    }

    private String extractFileExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        return extension.toLowerCase(Locale.ROOT);
    }

    private OptimizedImage optimizeImageIfPossible(MultipartFile file, StorageCategory category, String extension) {
        if (!imageProcessingEnabled || category != StorageCategory.IMAGE) {
            return null;
        }
        if (extension == null || !RESIZABLE_IMAGE_EXTENSIONS.contains(extension)) {
            return null;
        }

        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage sourceImage = ImageIO.read(inputStream);
            if (sourceImage == null) {
                return null;
            }

            BufferedImage resizedImage = resizeIfRequired(sourceImage);
            if (resizedImage == sourceImage) {
                return null;
            }

            if (resizedImage.getColorModel() != null && resizedImage.getColorModel().hasAlpha()) {
                return new OptimizedImage(encodePng(resizedImage), PNG_EXTENSION, "image/png");
            }

            BufferedImage rgbImage = ensureRgbImage(resizedImage);
            return new OptimizedImage(encodeJpeg(rgbImage, jpegQuality), JPEG_EXTENSION, "image/jpeg");
        } catch (IOException e) {
            throw new StorageException("이미지 리사이징에 실패했습니다.", e);
        }
    }

    private BufferedImage resizeIfRequired(BufferedImage sourceImage) {
        int sourceWidth = sourceImage.getWidth();
        int sourceHeight = sourceImage.getHeight();
        if (sourceWidth <= maxImageWidth && sourceHeight <= maxImageHeight) {
            return sourceImage;
        }

        double scale = Math.min((double) maxImageWidth / sourceWidth, (double) maxImageHeight / sourceHeight);
        int targetWidth = Math.max(1, (int) Math.round(sourceWidth * scale));
        int targetHeight = Math.max(1, (int) Math.round(sourceHeight * scale));

        int imageType = sourceImage.getColorModel() != null && sourceImage.getColorModel().hasAlpha()
                ? BufferedImage.TYPE_INT_ARGB
                : BufferedImage.TYPE_INT_RGB;
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, imageType);
        Graphics2D graphics = resizedImage.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(sourceImage, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }
        return resizedImage;
    }

    private BufferedImage ensureRgbImage(BufferedImage image) {
        if (image.getType() == BufferedImage.TYPE_INT_RGB) {
            return image;
        }

        BufferedImage rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgbImage.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, rgbImage.getWidth(), rgbImage.getHeight());
            graphics.drawImage(image, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return rgbImage;
    }

    private byte[] encodePng(BufferedImage image) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        boolean written = ImageIO.write(image, "png", outputStream);
        if (!written) {
            throw new StorageException("PNG 인코딩을 지원하지 않는 환경입니다.");
        }
        return outputStream.toByteArray();
    }

    private byte[] encodeJpeg(BufferedImage image, float quality) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            boolean written = ImageIO.write(image, "jpg", outputStream);
            if (!written) {
                throw new StorageException("JPEG 인코딩을 지원하지 않는 환경입니다.");
            }
            return outputStream.toByteArray();
        }

        ImageWriter writer = writers.next();
        try (ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream)) {
            writer.setOutput(imageOutputStream);
            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            if (writeParam.canWriteCompressed()) {
                writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                writeParam.setCompressionQuality(quality);
            }
            writer.write(null, new IIOImage(image, null, null), writeParam);
        } finally {
            writer.dispose();
        }
        return outputStream.toByteArray();
    }

    private float clampJpegQuality(float quality) {
        if (Float.isNaN(quality) || Float.isInfinite(quality)) {
            return 0.85f;
        }
        if (quality < 0.1f) {
            return 0.1f;
        }
        if (quality > 1.0f) {
            return 1.0f;
        }
        return quality;
    }

    private void storeOriginalImageCopy(MultipartFile file, Path categoryDir, String savedFilename, String originalExtension) {
        try {
            Path originalDir = categoryDir.resolve(ORIGINAL_IMAGE_DIRECTORY).normalize();
            Files.createDirectories(originalDir);
            String originalSavedFilename = buildOriginalSavedFilename(savedFilename, originalExtension);
            Path originalTargetPath = originalDir.resolve(originalSavedFilename).normalize();
            Files.write(originalTargetPath, file.getBytes());
        } catch (IOException e) {
            log.warn("원본 이미지 사본 저장에 실패했습니다. filename={}", savedFilename, e);
        }
    }

    private void deleteOriginalImageCopies(Path categoryDir, String savedFilename) {
        String baseName = extractBaseFilename(savedFilename);
        if (baseName.isBlank()) {
            return;
        }
        Path originalDir = categoryDir.resolve(ORIGINAL_IMAGE_DIRECTORY).normalize();
        if (!Files.isDirectory(originalDir)) {
            return;
        }

        String pattern = baseName + ORIGINAL_IMAGE_SUFFIX + ".*";
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(originalDir, pattern)) {
            for (Path originalPath : stream) {
                Files.deleteIfExists(originalPath);
            }
        } catch (IOException e) {
            log.warn("원본 이미지 사본 삭제에 실패했습니다. filename={}", savedFilename, e);
        }
    }

    private String buildOriginalSavedFilename(String savedFilename, String originalExtension) {
        String baseName = extractBaseFilename(savedFilename);
        String extension = (originalExtension == null || originalExtension.isBlank()) ? ".bin" : originalExtension;
        return baseName + ORIGINAL_IMAGE_SUFFIX + extension;
    }

    private String extractBaseFilename(String filename) {
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

    private Path resolveUploadPath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            throw new IllegalArgumentException("file.dir 값이 비어 있습니다.");
        }

        String normalized = rawPath.trim();
        if (normalized.matches("^/[A-Za-z]:/.*")) {
            normalized = normalized.substring(1);
        }
        return Path.of(normalized).toAbsolutePath().normalize();
    }

    private Path resolvePathUnderDirectory(Path baseDirectory, String filename) {
        if (baseDirectory == null || filename == null || filename.isBlank()) {
            return null;
        }
        String normalizedFilename = filename.trim();
        if (normalizedFilename.contains("/") || normalizedFilename.contains("\\") || normalizedFilename.contains("\0")) {
            return null;
        }
        Path resolved = baseDirectory.resolve(normalizedFilename).normalize();
        return resolved.startsWith(baseDirectory) ? resolved : null;
    }

    private record OptimizedImage(byte[] bytes, String fileExtension, String contentType) {
    }
}
