package kwh.PublicCookedFood.board.service;

import org.springframework.transaction.annotation.Transactional;
import kwh.PublicCookedFood.board.domain.BoardSection;
import kwh.PublicCookedFood.board.error.BoardSectionErrorCode;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import kwh.PublicCookedFood.board.repository.BoardSectionRepository;
import kwh.PublicCookedFood.common.error.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoardSectionService {

    private static final String DEFAULT_SECTION_KEY = "general";
    private static final String DEFAULT_SECTION_NAME = "일반";
    private static final Integer DEFAULT_DISPLAY_ORDER = 0;
    private static final String GENERATED_SECTION_KEY_PREFIX = "section";
    private static final int SECTION_KEY_MAX_LENGTH = 50;

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
        String normalizedName = normalizeSectionName(sectionName);
        String normalizedKey = resolveNewSectionKey(sectionKey, normalizedName);
        Integer resolvedDisplayOrder = displayOrder;
        if (resolvedDisplayOrder == null) {
            resolvedDisplayOrder = nextDisplayOrder();
        }

        BoardSection section = BoardSection.builder()
                .sectionKey(normalizedKey)
                .sectionName(normalizedName)
                .displayOrder(resolvedDisplayOrder)
                .active(true)
                .build();
        return boardSectionRepository.save(section);
    }

    @Transactional
    public BoardSection updateSection(Long sectionId, String sectionName, Integer displayOrder, Boolean active) {
        BoardSection section = boardSectionRepository.findById(sectionId)
                .orElseThrow(() -> new AppException(BoardSectionErrorCode.BOARD_SECTION_NOT_FOUND));

        if (DEFAULT_SECTION_KEY.equals(section.getSectionKey()) && Boolean.FALSE.equals(active)) {
            throw new AppException(BoardSectionErrorCode.BOARD_SECTION_DEFAULT_DEACTIVATE_FORBIDDEN);
        }

        String normalizedName = sectionName == null ? null : sectionName.trim();
        section.update(normalizedName, displayOrder, active);
        return section;
    }

    @Transactional
    public void deleteSection(Long sectionId) {
        BoardSection section = boardSectionRepository.findById(sectionId)
                .orElseThrow(() -> new AppException(BoardSectionErrorCode.BOARD_SECTION_NOT_FOUND));

        if (DEFAULT_SECTION_KEY.equals(section.getSectionKey())) {
            throw new AppException(BoardSectionErrorCode.BOARD_SECTION_DEFAULT_DELETE_FORBIDDEN);
        }

        BoardSection defaultSection = ensureDefaultSection();
        boardRepository.reassignSection(section, defaultSection);
        boardSectionRepository.delete(section);
    }

    @Transactional
    public List<BoardSection> reorderSections(List<Long> sectionIds) {
        if (sectionIds == null || sectionIds.isEmpty()) {
            throw new AppException(BoardSectionErrorCode.BOARD_SECTION_REORDER_INVALID);
        }

        Set<Long> orderedIds = new LinkedHashSet<>();
        for (Long sectionId : sectionIds) {
            if (sectionId == null || !orderedIds.add(sectionId)) {
                throw new AppException(BoardSectionErrorCode.BOARD_SECTION_REORDER_INVALID);
            }
        }

        Map<Long, BoardSection> sectionMap = getAllSections().stream()
                .collect(Collectors.toMap(BoardSection::getId, Function.identity()));
        if (sectionMap.size() != orderedIds.size() || !sectionMap.keySet().equals(orderedIds)) {
            throw new AppException(BoardSectionErrorCode.BOARD_SECTION_NOT_FOUND);
        }

        List<BoardSection> orderedSections = new ArrayList<>(orderedIds.size());
        int displayOrder = 0;
        for (Long sectionId : orderedIds) {
            BoardSection section = sectionMap.get(sectionId);
            section.update(null, displayOrder++, null);
            orderedSections.add(section);
        }
        return orderedSections;
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

    private String normalizeSectionName(String sectionName) {
        if (sectionName == null || sectionName.isBlank()) {
            throw new IllegalArgumentException("게시판 이름은 비어 있을 수 없습니다.");
        }
        return sectionName.trim();
    }

    private String resolveNewSectionKey(String sectionKey, String sectionName) {
        if (sectionKey != null && !sectionKey.isBlank()) {
            String normalizedKey = normalizeKey(sectionKey);
            if (boardSectionRepository.existsBySectionKey(normalizedKey)) {
                throw new AppException(BoardSectionErrorCode.BOARD_SECTION_KEY_DUPLICATED);
            }
            return normalizedKey;
        }

        String baseKey = buildGeneratedSectionKeyBase(sectionName);
        String candidate = baseKey;
        int suffix = 2;
        while (boardSectionRepository.existsBySectionKey(candidate)) {
            String suffixText = "-" + suffix++;
            candidate = truncate(baseKey, SECTION_KEY_MAX_LENGTH - suffixText.length()) + suffixText;
        }
        return candidate;
    }

    private String buildGeneratedSectionKeyBase(String sectionName) {
        String normalized = Normalizer.normalize(sectionName, Normalizer.Form.NFKD)
                .toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder();
        boolean previousSeparator = false;
        for (int i = 0; i < normalized.length(); i++) {
            char current = normalized.charAt(i);
            if ((current >= 'a' && current <= 'z') || (current >= '0' && current <= '9')) {
                builder.append(current);
                previousSeparator = false;
                continue;
            }
            if ((Character.isWhitespace(current) || current == '-' || current == '_') && builder.length() > 0 && !previousSeparator) {
                builder.append('-');
                previousSeparator = true;
            }
        }

        String sanitized = builder.toString().replaceAll("-+$", "");
        if (sanitized.isBlank()) {
            return GENERATED_SECTION_KEY_PREFIX;
        }
        return truncate(sanitized, SECTION_KEY_MAX_LENGTH);
    }

    private int nextDisplayOrder() {
        return boardSectionRepository.findFirstByOrderByDisplayOrderDescIdDesc()
                .map(BoardSection::getDisplayOrder)
                .orElse(-1) + 1;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return Objects.requireNonNullElse(value, "");
        }
        return value.substring(0, maxLength);
    }
}
