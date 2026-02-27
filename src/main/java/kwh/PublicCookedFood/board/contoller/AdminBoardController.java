package kwh.PublicCookedFood.board.contoller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kwh.PublicCookedFood.board.dto.request.BoardSectionCreateRequest;
import kwh.PublicCookedFood.board.dto.request.BoardSectionUpdateRequest;
import kwh.PublicCookedFood.board.dto.request.FeaturedThresholdUpdateRequest;
import kwh.PublicCookedFood.board.dto.request.ThumbnailDisplayModeUpdateRequest;
import kwh.PublicCookedFood.board.dto.response.BoardPolicyResponse;
import kwh.PublicCookedFood.board.dto.response.BoardSectionResponse;
import kwh.PublicCookedFood.board.facade.BoardAdminApiFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/boards")
@Tag(name = "Admin Board API")
public class AdminBoardController {

    private final BoardAdminApiFacade boardAdminApiFacade;

    @GetMapping("/sections")
    public ResponseEntity<List<BoardSectionResponse>> getSections() {
        return ResponseEntity.ok(boardAdminApiFacade.getSections());
    }

    @PostMapping("/sections")
    public ResponseEntity<BoardSectionResponse> createSection(@Valid @RequestBody BoardSectionCreateRequest request) {
        BoardSectionResponse response = boardAdminApiFacade.createSection(
                request.getSectionKey(),
                request.getSectionName(),
                request.getDisplayOrder()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/sections/{sectionId}")
    public ResponseEntity<BoardSectionResponse> updateSection(@PathVariable Long sectionId,
                                                              @Valid @RequestBody BoardSectionUpdateRequest request) {
        BoardSectionResponse response = boardAdminApiFacade.updateSection(
                sectionId,
                request.getSectionName(),
                request.getDisplayOrder(),
                request.getActive()
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/sections/{sectionId}")
    public ResponseEntity<Void> deleteSection(@PathVariable Long sectionId) {
        boardAdminApiFacade.deleteSection(sectionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/policy")
    public ResponseEntity<BoardPolicyResponse> getBoardPolicy() {
        return ResponseEntity.ok(boardAdminApiFacade.getBoardPolicy());
    }

    @PatchMapping("/policy/featured-threshold")
    public ResponseEntity<BoardPolicyResponse> updateFeaturedThreshold(
            @Valid @RequestBody FeaturedThresholdUpdateRequest request
    ) {
        return ResponseEntity.ok(boardAdminApiFacade.updateFeaturedThreshold(request.getFeaturedLikeThreshold()));
    }

    @PatchMapping("/policy/thumbnail-display-mode")
    public ResponseEntity<BoardPolicyResponse> updateThumbnailDisplayMode(
            @Valid @RequestBody ThumbnailDisplayModeUpdateRequest request
    ) {
        return ResponseEntity.ok(boardAdminApiFacade.updateThumbnailDisplayMode(request.getThumbnailDisplayMode()));
    }
}
