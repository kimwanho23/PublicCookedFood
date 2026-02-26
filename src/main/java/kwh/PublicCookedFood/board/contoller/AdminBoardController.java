package kwh.PublicCookedFood.board.contoller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kwh.PublicCookedFood.board.domain.BoardPolicy;
import kwh.PublicCookedFood.board.domain.BoardSection;
import kwh.PublicCookedFood.board.dto.request.BoardSectionCreateRequest;
import kwh.PublicCookedFood.board.dto.request.BoardSectionUpdateRequest;
import kwh.PublicCookedFood.board.dto.request.FeaturedThresholdUpdateRequest;
import kwh.PublicCookedFood.board.dto.request.ThumbnailDisplayModeUpdateRequest;
import kwh.PublicCookedFood.board.dto.response.BoardPolicyResponse;
import kwh.PublicCookedFood.board.dto.response.BoardSectionResponse;
import kwh.PublicCookedFood.board.service.BoardPolicyService;
import kwh.PublicCookedFood.board.service.BoardSectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/boards")
@Tag(name = "Admin Board API")
public class AdminBoardController {

    private final BoardSectionService boardSectionService;
    private final BoardPolicyService boardPolicyService;

    @GetMapping("/sections")
    public ResponseEntity<List<BoardSectionResponse>> getSections() {
        List<BoardSectionResponse> responses = boardSectionService.getAllSections().stream()
                .map(BoardSectionResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/sections")
    public ResponseEntity<BoardSectionResponse> createSection(@Valid @RequestBody BoardSectionCreateRequest request) {
        BoardSection section = boardSectionService.createSection(
                request.getSectionKey(),
                request.getSectionName(),
                request.getDisplayOrder()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(BoardSectionResponse.from(section));
    }

    @PatchMapping("/sections/{sectionId}")
    public ResponseEntity<BoardSectionResponse> updateSection(@PathVariable Long sectionId,
                                                              @Valid @RequestBody BoardSectionUpdateRequest request) {
        BoardSection section = boardSectionService.updateSection(
                sectionId,
                request.getSectionName(),
                request.getDisplayOrder(),
                request.getActive()
        );
        return ResponseEntity.ok(BoardSectionResponse.from(section));
    }

    @DeleteMapping("/sections/{sectionId}")
    public ResponseEntity<Void> deleteSection(@PathVariable Long sectionId) {
        boardSectionService.deleteSection(sectionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/policy")
    public ResponseEntity<BoardPolicyResponse> getBoardPolicy() {
        return ResponseEntity.ok(BoardPolicyResponse.from(boardPolicyService.getPolicy()));
    }

    @PatchMapping("/policy/featured-threshold")
    public ResponseEntity<BoardPolicyResponse> updateFeaturedThreshold(
            @Valid @RequestBody FeaturedThresholdUpdateRequest request
    ) {
        BoardPolicy boardPolicy = boardPolicyService.updateFeaturedLikeThreshold(request.getFeaturedLikeThreshold());
        return ResponseEntity.ok(BoardPolicyResponse.from(boardPolicy));
    }

    @PatchMapping("/policy/thumbnail-display-mode")
    public ResponseEntity<BoardPolicyResponse> updateThumbnailDisplayMode(
            @Valid @RequestBody ThumbnailDisplayModeUpdateRequest request
    ) {
        BoardPolicy boardPolicy = boardPolicyService.updateThumbnailDisplayMode(request.getThumbnailDisplayMode());
        return ResponseEntity.ok(BoardPolicyResponse.from(boardPolicy));
    }
}
