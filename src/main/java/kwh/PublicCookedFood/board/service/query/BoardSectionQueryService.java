package kwh.PublicCookedFood.board.service.query;

import kwh.PublicCookedFood.board.domain.BoardSection;
import kwh.PublicCookedFood.board.repository.BoardSectionRepository;
import kwh.PublicCookedFood.board.service.command.BoardSectionKeyValue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BoardSectionQueryService {

    private static final String DEFAULT_SECTION_KEY = "general";
    private static final String DEFAULT_SECTION_NAME = "일반";

    private final BoardSectionRepository boardSectionRepository;

    @Transactional(readOnly = true)
    public List<BoardSection> getActiveSections() {
        List<BoardSection> sections = boardSectionRepository.findAllByActiveTrueOrderByDisplayOrderAscIdAsc();
        if (sections.isEmpty()) {
            return List.of(defaultSection());
        }
        return sections;
    }

    @Transactional(readOnly = true)
    public List<BoardSection> getAllSections() {
        List<BoardSection> sections = boardSectionRepository.findAllByOrderByDisplayOrderAscIdAsc();
        if (sections.isEmpty()) {
            return List.of(defaultSection());
        }
        return sections;
    }

    @Transactional(readOnly = true)
    public Optional<BoardSection> findActiveSection(String sectionKey) {
        return BoardSectionKeyValue.parse(sectionKey)
                .flatMap(this::findActiveSection);
    }

    @Transactional(readOnly = true)
    public boolean existsActiveSection(String sectionKey) {
        return findActiveSection(sectionKey).isPresent();
    }

    private Optional<BoardSection> findActiveSection(BoardSectionKeyValue sectionKey) {
        Optional<BoardSection> persistedSection = boardSectionRepository.findBySectionKeyAndActiveTrue(sectionKey.value());
        if (persistedSection.isPresent()) {
            return persistedSection;
        }
        if (DEFAULT_SECTION_KEY.equals(sectionKey.value())) {
            return Optional.of(defaultSection());
        }
        return Optional.empty();
    }

    private BoardSection defaultSection() {
        return BoardSection.createDefault(DEFAULT_SECTION_KEY, DEFAULT_SECTION_NAME);
    }
}
