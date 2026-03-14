package kwh.PublicCookedFood.board.service;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.service.image.BoardImageSyncCommand;
import kwh.PublicCookedFood.board.service.image.BoardImagesZipQuery;
import kwh.PublicCookedFood.board.service.image.BoardImagesZipResource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BoardImageService {

    private final ImageReferenceService imageReferenceService;
    private final BoardImageArchiveService boardImageArchiveService;

    public void syncBoardImages(BoardImageSyncCommand command) {
        imageReferenceService.syncBoardImages(command);
    }

    public void deleteBoardImages(Board board) {
        imageReferenceService.deleteBoardImages(board);
    }

    public Optional<BoardImagesZipResource> findBoardImagesForZip(BoardImagesZipQuery query) {
        return boardImageArchiveService.findBoardImagesForZip(query);
    }
}
