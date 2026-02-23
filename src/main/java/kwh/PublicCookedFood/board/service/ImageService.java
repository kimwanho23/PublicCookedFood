package kwh.PublicCookedFood.board.service;

import jakarta.transaction.Transactional;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.Images;
import kwh.PublicCookedFood.board.domain.Images.ImageStatus;
import kwh.PublicCookedFood.board.repository.ImagesRepository;
import kwh.PublicCookedFood.storage.StorageCategory;
import kwh.PublicCookedFood.storage.StorageService;
import kwh.PublicCookedFood.storage.StoredResource;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");

    private final StorageService storageService;
    private final ImagesRepository imagesRepository;

    @Transactional
    public String uploadTempImage(MultipartFile file) {
        validateImageFile(file);
        StoredResource stored = storageService.store(file, StorageCategory.IMAGE);
        Images savedImage = Images.createTemporary(
                stored.originalFilename(),
                stored.savedFilename(),
                stored.resourceUrl(),
                stored.contentType(),
                stored.fileSize()
        );
        imagesRepository.save(savedImage);
        return stored.resourceUrl();
    }

    @Transactional
    public void syncBoardImages(Board board) {
        syncBoardImages(board, board.getContents());
    }

    @Transactional
    public void syncBoardImages(Board board, String htmlContent) {
        Set<String> currentImageUrls = extractLocalImageUrls(htmlContent);
        List<Images> attachedImages = imagesRepository.findAllByBoardAndStatus(board, ImageStatus.ATTACHED);
        Map<String, Images> attachedImageByUrl = attachedImages.stream()
                .collect(Collectors.toMap(Images::getImgUrl, Function.identity(), (left, right) -> left));

        for (String imageUrl : currentImageUrls) {
            if (attachedImageByUrl.containsKey(imageUrl)) {
                continue;
            }
            imagesRepository.findByImgUrlAndStatus(imageUrl, ImageStatus.TEMP)
                    .ifPresent(image -> image.attachTo(board));
        }

        for (Images attachedImage : attachedImages) {
            if (currentImageUrls.contains(attachedImage.getImgUrl())) {
                continue;
            }
            attachedImage.markDeleted();
            storageService.delete(StorageCategory.IMAGE, attachedImage.getSavedFilename());
        }
    }

    @Transactional
    public void deleteBoardImages(Board board) {
        List<Images> attachedImages = imagesRepository.findAllByBoardAndStatus(board, ImageStatus.ATTACHED);
        for (Images attachedImage : attachedImages) {
            attachedImage.markDeleted();
            storageService.delete(StorageCategory.IMAGE, attachedImage.getSavedFilename());
        }
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어 있습니다.");
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || !originalFileName.contains(".")) {
            throw new IllegalArgumentException("잘못된 파일명입니다.");
        }

        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf(".")).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(fileExtension)) {
            throw new IllegalArgumentException("지원하지 않는 확장자입니다.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다.");
        }
    }

    private Set<String> extractLocalImageUrls(String htmlContent) {
        if (htmlContent == null || htmlContent.isBlank()) {
            return Set.of();
        }

        Document document = Jsoup.parse(htmlContent);
        Set<String> imageUrls = new LinkedHashSet<>();
        for (Element imageTag : document.select("img[src]")) {
            String src = imageTag.attr("src");
            normalizeLocalImageUrl(src).ifPresent(imageUrls::add);
        }
        return imageUrls;
    }

    private Optional<String> normalizeLocalImageUrl(String src) {
        if (src == null || src.isBlank()) {
            return Optional.empty();
        }

        String trimmedSrc = src.trim();
        if (trimmedSrc.startsWith(StorageCategory.IMAGE.getUrlPrefix())) {
            return Optional.of(trimmedSrc);
        }

        if (trimmedSrc.startsWith("http://") || trimmedSrc.startsWith("https://")) {
            try {
                String path = URI.create(trimmedSrc).getPath();
                if (path != null && path.startsWith(StorageCategory.IMAGE.getUrlPrefix())) {
                    return Optional.of(path);
                }
            } catch (IllegalArgumentException ignored) {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }
}
