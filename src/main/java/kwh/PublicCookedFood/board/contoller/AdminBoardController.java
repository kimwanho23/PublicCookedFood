package kwh.PublicCookedFood.board.contoller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kwh.PublicCookedFood.board.dto.request.BoardPolicyUpdateRequest;
import kwh.PublicCookedFood.board.dto.request.BoardSectionCreateRequest;
import kwh.PublicCookedFood.board.dto.request.BoardSectionReorderRequest;
import kwh.PublicCookedFood.board.dto.request.BoardSectionUpdateRequest;
import kwh.PublicCookedFood.board.dto.request.FeaturedThresholdUpdateRequest;
import kwh.PublicCookedFood.board.dto.request.ThumbnailDisplayModeUpdateRequest;
import kwh.PublicCookedFood.board.dto.response.BoardPolicyResponse;
import kwh.PublicCookedFood.board.dto.response.BoardSectionResponse;
import kwh.PublicCookedFood.board.facade.BoardAdminApiFacade;
import kwh.PublicCookedFood.config.oauth2.LoginAccount;
import kwh.PublicCookedFood.account.domain.Account;
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
@Tag(name = "게시판 관리자 API", description = "게시판 탭과 운영 정책을 관리하는 관리자용 API")
public class AdminBoardController {

    private final BoardAdminApiFacade boardAdminApiFacade;

    @Operation(summary = "게시판 탭 목록 조회", description = "관리자가 사용하는 게시판 탭 목록을 조회합니다.")
    @GetMapping("/sections")
    public ResponseEntity<List<BoardSectionResponse>> getSections() {
        return ResponseEntity.ok(boardAdminApiFacade.getSections());
    }

    @Operation(summary = "게시판 탭 생성", description = "새 게시판 탭을 생성합니다. 키를 생략하면 이름으로 자동 생성합니다.")
    @PostMapping("/sections")
    public ResponseEntity<BoardSectionResponse> createSection(@Valid @RequestBody BoardSectionCreateRequest request) {
        BoardSectionResponse response = boardAdminApiFacade.createSection(
                request.getSectionKey(),
                request.getSectionName(),
                request.getDisplayOrder()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "게시판 섹션 순서 변경", description = "게시판 섹션의 노출 순서를 요청한 순서대로 다시 저장합니다.")
    @PatchMapping("/sections/reorder")
    public ResponseEntity<List<BoardSectionResponse>> reorderSections(
            @Valid @RequestBody BoardSectionReorderRequest request
    ) {
        return ResponseEntity.ok(boardAdminApiFacade.reorderSections(request.getSectionIds()));
    }

    @PatchMapping("/sections/{sectionId}")
    @Operation(summary = "게시판 탭 수정", description = "게시판 탭 이름, 노출 순서, 활성 여부를 수정합니다.")
    public ResponseEntity<BoardSectionResponse> updateSection(
            @Parameter(description = "수정할 게시판 탭 ID", required = true)
            @PathVariable Long sectionId,
            @Valid @RequestBody BoardSectionUpdateRequest request
    ) {
        BoardSectionResponse response = boardAdminApiFacade.updateSection(
                sectionId,
                request.getSectionName(),
                request.getDisplayOrder(),
                request.getActive()
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "게시판 탭 삭제", description = "선택한 게시판 탭을 삭제하고 관련 게시글을 일반 탭으로 이동합니다.")
    @DeleteMapping("/sections/{sectionId}")
    public ResponseEntity<Void> deleteSection(
            @Parameter(description = "삭제할 게시판 탭 ID", required = true)
            @PathVariable Long sectionId
    ) {
        boardAdminApiFacade.deleteSection(sectionId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "게시판 정책 일괄 수정", description = "추천 기준값과 썸네일 표시 모드를 한 번에 수정합니다.")
    @PatchMapping("/policy")
    public ResponseEntity<BoardPolicyResponse> updatePolicy(
            @Valid @RequestBody BoardPolicyUpdateRequest request,
            @Parameter(hidden = true) @LoginAccount Account account
    ) {
        Long actorAccountId = account == null ? null : account.getId();
        return ResponseEntity.ok(boardAdminApiFacade.updatePolicy(
                request.getFeaturedLikeThreshold(),
                request.getThumbnailDisplayMode(),
                actorAccountId
        ));
    }

    @Operation(summary = "인기 게시글 기준 수정", description = "인기 게시글로 분류할 좋아요 기준값을 수정합니다.")
    @PatchMapping("/policy/featured-threshold")
    public ResponseEntity<BoardPolicyResponse> updateFeaturedThreshold(
            @Valid @RequestBody FeaturedThresholdUpdateRequest request,
            @Parameter(hidden = true) @LoginAccount Account account
    ) {
        Long actorAccountId = account == null ? null : account.getId();
        return ResponseEntity.ok(boardAdminApiFacade.updateFeaturedThreshold(request.getFeaturedLikeThreshold(), actorAccountId));
    }

    @Operation(summary = "썸네일 노출 방식 수정", description = "게시판 목록에서 사용하는 썸네일 노출 방식을 변경합니다.")
    @PatchMapping("/policy/thumbnail-display-mode")
    public ResponseEntity<BoardPolicyResponse> updateThumbnailDisplayMode(
            @Valid @RequestBody ThumbnailDisplayModeUpdateRequest request,
            @Parameter(hidden = true) @LoginAccount Account account
    ) {
        Long actorAccountId = account == null ? null : account.getId();
        return ResponseEntity.ok(boardAdminApiFacade.updateThumbnailDisplayMode(request.getThumbnailDisplayMode(), actorAccountId));
    }
}
