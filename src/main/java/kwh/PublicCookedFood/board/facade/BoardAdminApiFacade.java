package kwh.PublicCookedFood.board.facade;

import kwh.PublicCookedFood.board.domain.BoardPolicy;
import kwh.PublicCookedFood.board.domain.BoardSection;
import kwh.PublicCookedFood.board.domain.BoardThumbnailDisplayMode;
import kwh.PublicCookedFood.board.dto.response.BoardPolicyResponse;
import kwh.PublicCookedFood.board.dto.response.BoardSectionResponse;
import kwh.PublicCookedFood.board.service.BoardPolicyService;
import kwh.PublicCookedFood.board.service.BoardSectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardAdminApiFacade {

    private final BoardSectionService boardSectionService;
    private final BoardPolicyService boardPolicyService;

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

    public BoardPolicyResponse getBoardPolicy() {
        return BoardPolicyResponse.from(boardPolicyService.getPolicy());
    }

    public BoardPolicyResponse updateFeaturedThreshold(Integer featuredLikeThreshold) {
        BoardPolicy boardPolicy = boardPolicyService.updateFeaturedLikeThreshold(featuredLikeThreshold);
        return BoardPolicyResponse.from(boardPolicy);
    }

    public BoardPolicyResponse updateThumbnailDisplayMode(BoardThumbnailDisplayMode thumbnailDisplayMode) {
        BoardPolicy boardPolicy = boardPolicyService.updateThumbnailDisplayMode(thumbnailDisplayMode);
        return BoardPolicyResponse.from(boardPolicy);
    }
}
