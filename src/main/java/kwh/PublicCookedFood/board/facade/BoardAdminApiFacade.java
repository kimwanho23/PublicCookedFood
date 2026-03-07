package kwh.PublicCookedFood.board.facade;

import kwh.PublicCookedFood.board.domain.BoardSection;
import kwh.PublicCookedFood.board.domain.BoardThumbnailDisplayMode;
import kwh.PublicCookedFood.board.dto.response.BoardPolicyResponse;
import kwh.PublicCookedFood.board.dto.response.BoardSectionResponse;
import kwh.PublicCookedFood.board.service.BoardSectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardAdminApiFacade {

    private final BoardSectionService boardSectionService;
    private final BoardAdminPolicyFacade boardAdminPolicyFacade;

    public List<BoardSectionResponse> getSections() {
        return boardSectionService.getAllSections().stream()
                .map(BoardSectionResponse::from)
                .toList();
    }

    public BoardSectionResponse createSection(String sectionKey,
                                              String sectionName,
                                              Integer displayOrder) {
        BoardSection section = boardSectionService.createSection(sectionKey, sectionName, displayOrder);
        return BoardSectionResponse.from(section);
    }

    public BoardSectionResponse updateSection(Long sectionId,
                                              String sectionName,
                                              Integer displayOrder,
                                              Boolean active) {
        BoardSection section = boardSectionService.updateSection(sectionId, sectionName, displayOrder, active);
        return BoardSectionResponse.from(section);
    }

    public void deleteSection(Long sectionId) {
        boardSectionService.deleteSection(sectionId);
    }

    public List<BoardSectionResponse> reorderSections(List<Long> sectionIds) {
        return boardSectionService.reorderSections(sectionIds).stream()
                .map(BoardSectionResponse::from)
                .toList();
    }

    public BoardPolicyResponse updatePolicy(Integer featuredLikeThreshold,
                                            BoardThumbnailDisplayMode thumbnailDisplayMode,
                                            Long actorAccountId) {
        boardAdminPolicyFacade.updateBoardPolicy(featuredLikeThreshold, thumbnailDisplayMode, actorAccountId);
        return loadPolicy();
    }

    public BoardPolicyResponse updateFeaturedThreshold(Integer featuredLikeThreshold, Long actorAccountId) {
        boardAdminPolicyFacade.updateBoardPolicy(featuredLikeThreshold, null, actorAccountId);
        return loadPolicy();
    }

    public BoardPolicyResponse updateThumbnailDisplayMode(BoardThumbnailDisplayMode thumbnailDisplayMode, Long actorAccountId) {
        boardAdminPolicyFacade.updateBoardPolicy(null, thumbnailDisplayMode, actorAccountId);
        return loadPolicy();
    }

    private BoardPolicyResponse loadPolicy() {
        BoardAdminFacade.PolicyViewData data = boardAdminPolicyFacade.loadPolicyData();
        return data.policy();
    }
}
