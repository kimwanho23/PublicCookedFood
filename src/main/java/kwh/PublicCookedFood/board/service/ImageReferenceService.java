package kwh.PublicCookedFood.board.service;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardImage;
import kwh.PublicCookedFood.board.domain.Images;
import kwh.PublicCookedFood.board.domain.Images.ImageStatus;
import kwh.PublicCookedFood.board.repository.BoardImageRepository;
import kwh.PublicCookedFood.board.repository.ImagesRepository;
import kwh.PublicCookedFood.storage.StorageCategory;
import kwh.PublicCookedFood.storage.StorageService;
import kwh.PublicCookedFood.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
                storageService.delete(StorageCategory.IMAGE, staleImage.getSavedFilename());
                deletedCount++;
            }
        }
        return deletedCount;
    }

    @Transactional
    public void attachProfileImageIfPresent(String imageUrl) {
        imageUrlSupport.normalizeLocalImageUrl(imageUrl)
                .flatMap(url -> imagesRepository.findByImgUrlAndStatusIn(url, LINKABLE_IMAGE_STATUSES))
                .ifPresent(Images::attach);
    }

    @Transactional
    public void cleanupImageByUrlIfUnlinked(String imageUrl) {
        imageUrlSupport.normalizeLocalImageUrl(imageUrl)
                .flatMap(url -> imagesRepository.findByImgUrlAndStatusIn(url, LINKABLE_IMAGE_STATUSES))
                .ifPresent(this::cleanupImageIfUnlinked);
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
        storageService.delete(StorageCategory.IMAGE, image.getSavedFilename());
    }

    private boolean isImageReferencedByAnyUser(Images image) {
        if (image == null) {
            return false;
        }
        String imageUrl = image.getImgUrl();
        if (imageUrl == null || imageUrl.isBlank()) {
            return false;
        }
        return accountRepository.existsByProfileImageUrl(imageUrl);
    }
}
