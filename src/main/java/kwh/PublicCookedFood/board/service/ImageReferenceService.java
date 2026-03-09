package kwh.PublicCookedFood.board.service;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardImage;
import kwh.PublicCookedFood.board.domain.Images;
import kwh.PublicCookedFood.board.domain.Images.ImageStatus;
import kwh.PublicCookedFood.board.domain.SoftDeleteState;
import kwh.PublicCookedFood.board.repository.BoardImageRepository;
import kwh.PublicCookedFood.board.repository.ImagesRepository;
import kwh.PublicCookedFood.account.repository.AccountRepository;
import kwh.PublicCookedFood.storage.StorageCategory;
import kwh.PublicCookedFood.storage.StorageService;
import kwh.PublicCookedFood.userrecipe.repository.UserRecipeRepository;
import kwh.PublicCookedFood.userrecipe.repository.UserRecipeReviewRepository;
import kwh.PublicCookedFood.userrecipe.repository.UserRecipeStepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ImageReferenceService {

    private static final Set<ImageStatus> LINKABLE_IMAGE_STATUSES = Set.of(ImageStatus.TEMP, ImageStatus.ATTACHED);

    private final ImagesRepository imagesRepository;
    private final BoardImageRepository boardImageRepository;
    private final AccountRepository accountRepository;
    private final UserRecipeRepository userRecipeRepository;
    private final UserRecipeStepRepository userRecipeStepRepository;
    private final UserRecipeReviewRepository userRecipeReviewRepository;
    private final ImageSchemaService imageSchemaService;
    private final StorageService storageService;
    private final ImageUrlSupport imageUrlSupport;

    @Transactional
    public void syncBoardImages(Board board, String htmlContent) {
        imageSchemaService.ensureBoardImageSchemaCompatibleIfEnabled();
        Set<String> currentImageUrls = imageUrlSupport.extractLocalImageUrls(htmlContent);
        List<BoardImage> boardImages = boardImageRepository.findAllByBoardWithImage(board);
        Map<String, BoardImage> boardImageByUrl = boardImages.stream()
                .filter(boardImage -> boardImage.getImage() != null && boardImage.getImage().getImgUrl() != null)
                .collect(Collectors.toMap(boardImage -> boardImage.getImage().getImgUrl(),
                        Function.identity(), (left, right) -> left));

        attachNewBoardImages(board, currentImageUrls, boardImageByUrl);
        detachRemovedBoardImages(boardImages, currentImageUrls);
    }

    @Transactional
    public void deleteBoardImages(Board board) {
        imageSchemaService.ensureBoardImageSchemaCompatibleIfEnabled();
        List<BoardImage> boardImages = boardImageRepository.findAllByBoardWithImage(board);
        for (BoardImage boardImage : boardImages) {
            Images image = boardImage.getImage();
            boardImageRepository.delete(boardImage);
            if (image != null) {
                cleanupImageIfUnlinked(image);
            }
        }
    }

    @Transactional
    public int deleteStaleTempImages(LocalDateTime cutoff, int batchSize) {
        imageSchemaService.ensureBoardImageSchemaCompatibleIfEnabled();
        if (cutoff == null || batchSize <= 0) {
            return 0;
        }

        Pageable pageable = PageRequest.of(0, batchSize);
        List<Images> staleTempImages = imagesRepository.findStaleImages(ImageStatus.TEMP, cutoff, pageable);

        int deletedCount = 0;
        for (Images staleImage : staleTempImages) {
            if (boardImageRepository.existsByImage(staleImage) || isImageReferencedByAnyUser(staleImage)) {
                continue;
            }
            int affected = imagesRepository.deleteByIdAndStatus(staleImage.getId(), ImageStatus.TEMP);
            if (affected > 0) {
                resolveStorageCategory(staleImage).ifPresent(category ->
                        storageService.delete(category, staleImage.getSavedFilename()));
                deletedCount++;
            }
        }
        return deletedCount;
    }

    @Transactional
    public void attachProfileImageIfPresent(String imageUrl) {
        attachImageIfPresent(imageUrl);
    }

    @Transactional
    public void attachImagesIfPresent(Set<String> imageUrls) {
        normalizeLocalImageUrls(imageUrls).forEach(this::attachImageIfPresent);
    }

    @Transactional
    public void cleanupImageByUrlIfUnlinked(String imageUrl) {
        cleanupNormalizedImageUrlIfUnlinked(normalizeImageUrl(imageUrl));
    }

    @Transactional
    public void cleanupImagesByUrlIfUnlinked(Set<String> imageUrls) {
        normalizeLocalImageUrls(imageUrls).forEach(imageUrl ->
                cleanupNormalizedImageUrlIfUnlinked(Optional.of(imageUrl)));
    }

    private void attachNewBoardImages(Board board,
                                      Set<String> currentImageUrls,
                                      Map<String, BoardImage> boardImageByUrl) {
        for (String imageUrl : currentImageUrls) {
            if (boardImageByUrl.containsKey(imageUrl)) {
                continue;
            }
            imagesRepository.findByImgUrlAndStatusIn(imageUrl, LINKABLE_IMAGE_STATUSES)
                    .ifPresent(image -> {
                        image.attach();
                        boardImageRepository.findByBoardAndImage(board, image)
                                .orElseGet(() -> boardImageRepository.save(BoardImage.of(board, image)));
                    });
        }
    }

    private void detachRemovedBoardImages(List<BoardImage> boardImages, Set<String> currentImageUrls) {
        for (BoardImage boardImage : boardImages) {
            Images image = boardImage.getImage();
            if (image == null) {
                boardImageRepository.delete(boardImage);
                continue;
            }
            if (currentImageUrls.contains(image.getImgUrl())) {
                continue;
            }
            boardImageRepository.delete(boardImage);
            cleanupImageIfUnlinked(image);
        }
    }

    private void cleanupImageIfUnlinked(Images image) {
        if (boardImageRepository.existsByImage(image) || isImageReferencedByAnyUser(image)) {
            return;
        }
        image.markDeleted();
        resolveStorageCategory(image).ifPresent(category ->
                storageService.delete(category, image.getSavedFilename()));
    }

    private boolean isImageReferencedByAnyUser(Images image) {
        if (image == null) {
            return false;
        }
        String imageUrl = image.getImgUrl();
        if (imageUrl == null || imageUrl.isBlank()) {
            return false;
        }
        return accountRepository.existsByProfileImageUrl(imageUrl)
                || userRecipeRepository.existsByThumbnailUrlAndState(imageUrl, SoftDeleteState.ACTIVE)
                || userRecipeStepRepository.existsByImageUrlAndRecipeState(imageUrl, SoftDeleteState.ACTIVE)
                || userRecipeReviewRepository.existsByImageUrlAndRecipeState(imageUrl, SoftDeleteState.ACTIVE);
    }

    private void attachImageIfPresent(String imageUrl) {
        normalizeImageUrl(imageUrl)
                .flatMap(url -> imagesRepository.findByImgUrlAndStatusIn(url, LINKABLE_IMAGE_STATUSES))
                .ifPresent(Images::attach);
    }

    private void cleanupNormalizedImageUrlIfUnlinked(Optional<String> imageUrl) {
        imageUrl.flatMap(url -> imagesRepository.findByImgUrlAndStatusIn(url, LINKABLE_IMAGE_STATUSES))
                .ifPresent(this::cleanupImageIfUnlinked);
    }

    private Optional<String> normalizeImageUrl(String imageUrl) {
        return imageUrlSupport.normalizeLocalImageUrl(imageUrl);
    }

    private Set<String> normalizeLocalImageUrls(Set<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return Set.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String imageUrl : imageUrls) {
            normalizeImageUrl(imageUrl).ifPresent(normalized::add);
        }
        return normalized;
    }

    private Optional<StorageCategory> resolveStorageCategory(Images image) {
        if (image == null) {
            return Optional.empty();
        }
        return StorageCategory.resolveByUrl(image.getImgUrl());
    }
}
