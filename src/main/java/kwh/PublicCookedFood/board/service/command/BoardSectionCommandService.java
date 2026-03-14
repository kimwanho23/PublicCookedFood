package kwh.PublicCookedFood.board.service.command;

import kwh.PublicCookedFood.board.domain.BoardSection;
import kwh.PublicCookedFood.board.error.BoardSectionErrorCode;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import kwh.PublicCookedFood.board.repository.BoardSectionRepository;
import kwh.PublicCookedFood.common.error.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoardSectionCommandService {

    private static final String DEFAULT_SECTION_KEY = "general";
    private static final String DEFAULT_SECTION_NAME = "일반";
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
                .orElseThrow(() -> new AppException(BoardSectionErrorCode.BOARD_SECTION_NOT_FOUND));
    }

    @Transactional
    public BoardSection createSection(BoardSectionCreateCommand command) {
        String normalizedName = command.sectionName().value();
        String normalizedKey = resolveNewSectionKey(command.sectionKey(), command.sectionName());
        Integer resolvedDisplayOrder = command.displayOrder();
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
    public BoardSection updateSection(BoardSectionUpdateCommand command) {
        BoardSection section = boardSectionRepository.findById(command.sectionId())
                .orElseThrow(() -> new AppException(BoardSectionErrorCode.BOARD_SECTION_NOT_FOUND));

        if (DEFAULT_SECTION_KEY.equals(section.getSectionKey()) && Boolean.FALSE.equals(command.active())) {
            throw new AppException(BoardSectionErrorCode.BOARD_SECTION_DEFAULT_DEACTIVATE_FORBIDDEN);
        }

        section.rename(command.sectionName().value());
        if (command.displayOrder() != null) {
            section.changeDisplayOrder(command.displayOrder());
        }
        if (command.active() != null) {
            if (command.active()) {
                section.activate();
            } else {
                section.deactivate();
            }
        }
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
    public List<BoardSection> reorderSections(BoardSectionReorderCommand command) {
        List<Long> sectionIds = command.sectionIds();
        if (sectionIds == null || sectionIds.isEmpty()) {
            throw new AppException(BoardSectionErrorCode.BOARD_SECTION_REORDER_INVALID);
        }

        Set<Long> orderedIds = new LinkedHashSet<>();
        for (Long sectionId : sectionIds) {
            if (sectionId == null || !orderedIds.add(sectionId)) {
                throw new AppException(BoardSectionErrorCode.BOARD_SECTION_REORDER_INVALID);
            }
        }

        Map<Long, BoardSection> sectionMap = loadAllSectionsForCommand().stream()
                .collect(Collectors.toMap(BoardSection::getId, Function.identity()));
        if (sectionMap.size() != orderedIds.size() || !sectionMap.keySet().equals(orderedIds)) {
            throw new AppException(BoardSectionErrorCode.BOARD_SECTION_NOT_FOUND);
        }

        List<BoardSection> orderedSections = new ArrayList<>(orderedIds.size());
        int displayOrder = 0;
        for (Long sectionId : orderedIds) {
            BoardSection section = sectionMap.get(sectionId);
            section.changeDisplayOrder(displayOrder++);
            orderedSections.add(section);
        }
        return orderedSections;
    }

    private List<BoardSection> loadAllSectionsForCommand() {
        List<BoardSection> sections = boardSectionRepository.findAllByOrderByDisplayOrderAscIdAsc();
        if (sections.isEmpty()) {
            return List.of(ensureDefaultSection());
        }
        return sections;
    }

    private String resolveNewSectionKey(BoardSectionKeyValue sectionKey, BoardSectionNameValue sectionName) {
        if (sectionKey != null) {
            if (boardSectionRepository.existsBySectionKey(sectionKey.value())) {
                throw new AppException(BoardSectionErrorCode.BOARD_SECTION_KEY_DUPLICATED);
            }
            return sectionKey.value();
        }

        String baseKey = buildGeneratedSectionKeyBase(sectionName.value());
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
