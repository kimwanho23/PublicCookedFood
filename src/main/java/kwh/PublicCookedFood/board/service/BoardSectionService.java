package kwh.PublicCookedFood.board.service;

import org.springframework.transaction.annotation.Transactional;
import kwh.PublicCookedFood.board.domain.BoardSection;
import kwh.PublicCookedFood.board.domain.SoftDeleteState;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import kwh.PublicCookedFood.board.repository.BoardSectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BoardSectionService {

    private static final String DEFAULT_SECTION_KEY = "general";
    private static final String DEFAULT_SECTION_NAME = "자유";

    private final BoardSectionRepository boardSectionRepository;
    private final BoardRepository boardRepository;

    @Transactional
    public BoardSection ensureDefaultSection() {
        return boardSectionRepository.findBySectionKey(DEFAULT_SECTION_KEY)
                .orElseGet(() -> boardSectionRepository.save(
                        BoardSection.createDefault(DEFAULT_SECTION_KEY, DEFAULT_SECTION_NAME)
                ));
    }

    @Transactional
    public BoardSection resolveSectionForWrite(Long sectionId) {
        if (sectionId == null) {
            return ensureDefaultSection();
        }
        return boardSectionRepository.findById(sectionId)
                .filter(BoardSection::getActive)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 게시판 탭입니다."));
    }

    @Transactional
    public BoardSection createSection(String sectionKey, String sectionName, Integer displayOrder) {
        String normalizedKey = normalizeKey(sectionKey);
        if (boardSectionRepository.existsBySectionKey(normalizedKey)) {
            throw new IllegalArgumentException("이미 존재하는 게시판 키입니다.");
        }

        BoardSection section = BoardSection.builder()
                .sectionKey(normalizedKey)
                .sectionName(sectionName.trim())
                .displayOrder(displayOrder == null ? 0 : displayOrder)
                .active(true)
                .build();
        return boardSectionRepository.save(section);
    }

    @Transactional
    public BoardSection updateSection(Long sectionId, String sectionName, Integer displayOrder, Boolean active) {
        BoardSection section = boardSectionRepository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("게시판 탭을 찾을 수 없습니다."));

        if (DEFAULT_SECTION_KEY.equals(section.getSectionKey()) && Boolean.FALSE.equals(active)) {
            throw new IllegalArgumentException("기본 게시판 탭은 비활성화할 수 없습니다.");
        }

        String normalizedName = sectionName == null ? null : sectionName.trim();
        section.update(normalizedName, displayOrder, active);
        return section;
    }

    @Transactional
    public void deleteSection(Long sectionId) {
        BoardSection section = boardSectionRepository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("게시판 탭을 찾을 수 없습니다."));

        if (DEFAULT_SECTION_KEY.equals(section.getSectionKey())) {
            throw new IllegalArgumentException("기본 게시판 탭은 삭제할 수 없습니다.");
        }

        if (boardRepository.existsBySectionAndState(section, SoftDeleteState.ACTIVE)) {
            throw new IllegalStateException("게시글이 남아 있는 탭은 삭제할 수 없습니다.");
        }

        boardSectionRepository.delete(section);
    }

    @Transactional
    public List<BoardSection> getActiveSections() {
        List<BoardSection> sections = boardSectionRepository.findAllByActiveTrueOrderByDisplayOrderAscIdAsc();
        if (sections.isEmpty()) {
            return List.of(ensureDefaultSection());
        }
        return sections;
    }

    @Transactional
    public List<BoardSection> getAllSections() {
        List<BoardSection> sections = boardSectionRepository.findAllByOrderByDisplayOrderAscIdAsc();
        if (sections.isEmpty()) {
            return List.of(ensureDefaultSection());
        }
        return sections;
    }

    @Transactional
    public Optional<BoardSection> findActiveSection(String sectionKey) {
        if (sectionKey == null || sectionKey.isBlank()) {
            return Optional.empty();
        }
        return boardSectionRepository.findBySectionKeyAndActiveTrue(normalizeKey(sectionKey));
    }

    @Transactional
    public boolean existsActiveSection(String sectionKey) {
        return findActiveSection(sectionKey).isPresent();
    }

    private String normalizeKey(String sectionKey) {
        if (sectionKey == null || sectionKey.isBlank()) {
            throw new IllegalArgumentException("게시판 키는 비어 있을 수 없습니다.");
        }
        return sectionKey.trim().toLowerCase(Locale.ROOT);
    }
}
