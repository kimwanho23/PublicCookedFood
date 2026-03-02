package kwh.PublicCookedFood.board.service;

import org.springframework.transaction.annotation.Transactional;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardSection;
import kwh.PublicCookedFood.board.domain.SoftDeleteState;
import kwh.PublicCookedFood.board.domain.BoardThumbnailDisplayMode;
import kwh.PublicCookedFood.board.dto.request.BoardSaveRequest;
import kwh.PublicCookedFood.board.dto.response.BoardDetailResponse;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import kwh.PublicCookedFood.board.repository.CommentsRepository;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.repository.UserRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoardService {

    private static final Safelist BOARD_CONTENT_SAFELIST = Safelist.relaxed()
            .addTags("iframe")
            .addAttributes("span", "style")
            .addAttributes("iframe", "src", "title", "width", "height", "frameborder", "allow", "allowfullscreen")
            .addProtocols("img", "src", "http", "https")
            .addProtocols("iframe", "src", "http", "https")
            .preserveRelativeLinks(true);
    private static final Set<String> ALLOWED_FONT_SIZES = Set.of(
            "12px",
            "14px",
            "16px",
            "18px",
            "20px",
            "24px",
            "28px",
            "32px"
    );
    private static final Map<String, String> ALLOWED_FONT_FAMILIES = createAllowedFontFamilies();

    private final BoardRepository boardRepository;
    private final CommentsRepository commentsRepository;
    private final ImageService imageService;
    private final BoardSectionService boardSectionService;
    private final BoardPolicyService boardPolicyService;
    private final UserRepository userRepository;

    public Page<Board> getBoardList(Pageable pageable, String sectionKey, Long authorId, Collection<Long> blockedUserIds) {
        BlockedUserFilter blockedUserFilter = resolveBlockedUserFilter(blockedUserIds);
        return boardRepository.findAllByStateWithUser(
                SoftDeleteState.ACTIVE,
                blockedUserFilter.excludeBlocked(),
                blockedUserFilter.blockedUserIds(),
                authorId,
                sectionKey,
                pageable);
    }

    public Page<Board> getFeaturedBoardList(Pageable pageable,
                                            String sectionKey,
                                            Long authorId,
                                            Collection<Long> blockedUserIds) {
        BlockedUserFilter blockedUserFilter = resolveBlockedUserFilter(blockedUserIds);
        long threshold = boardPolicyService.getFeaturedLikeThreshold();
        return boardRepository.findFeaturedByStateWithUser(
                SoftDeleteState.ACTIVE,
                blockedUserFilter.excludeBlocked(),
                blockedUserFilter.blockedUserIds(),
                authorId,
                threshold,
                sectionKey,
                pageable);
    }

    public BoardDetailResponse getBoardDetail(Long id){
        Board board = boardRepository.findByIdWithUserAndState(id, SoftDeleteState.ACTIVE)
                .orElseThrow(() -> new NoSuchElementException("유효한 게시글을 찾을 수 없습니다."));
        return BoardDetailResponse.from(board);
    }

    @Transactional(readOnly = true)
    public Page<Board> findByKeyword(String search,
                                     String sectionKey,
                                     Long authorId,
                                     Collection<Long> blockedUserIds,
                                     Pageable pageable) {
        BlockedUserFilter blockedUserFilter = resolveBlockedUserFilter(blockedUserIds);
        return boardRepository.findByTitleContainingAndStateWithUser(
                search,
                SoftDeleteState.ACTIVE,
                blockedUserFilter.excludeBlocked(),
                blockedUserFilter.blockedUserIds(),
                authorId,
                sectionKey,
                pageable);
    }

    @Transactional(readOnly = true)
    public Page<Board> findFeaturedByKeyword(String search,
                                             String sectionKey,
                                             Long authorId,
                                             Collection<Long> blockedUserIds,
                                             Pageable pageable) {
        BlockedUserFilter blockedUserFilter = resolveBlockedUserFilter(blockedUserIds);
        long threshold = boardPolicyService.getFeaturedLikeThreshold();
        return boardRepository.findFeaturedByTitleContainingAndStateWithUser(
                search,
                SoftDeleteState.ACTIVE,
                blockedUserFilter.excludeBlocked(),
                blockedUserFilter.blockedUserIds(),
                authorId,
                threshold,
                sectionKey,
                pageable);
    }

    @Transactional
    public Board save(BoardSaveRequest boardDto) {
        String rawContents = boardDto.getContents();
        String sanitizedTitle = Jsoup.clean(boardDto.getTitle() == null ? "" : boardDto.getTitle(), Safelist.none());
        String sanitizedContents = sanitizeBoardContents(rawContents);
        BoardSection section = boardSectionService.resolveSectionForWrite(boardDto.getSectionId());
        Users user = userRepository.findById(boardDto.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid user ID"));
        boolean hiddenByReport = false;
        if (boardDto.getId() != null) {
            hiddenByReport = boardRepository.findById(boardDto.getId())
                    .map(Board::isHiddenByReport)
                    .orElse(false);
        }
        boardDto.setTitle(sanitizedTitle);
        boardDto.setContents(sanitizedContents);
        Board board = Board.builder()
                .id(boardDto.getId())
                .title(boardDto.getTitle())
                .contents(boardDto.getContents())
                .user(user)
                .section(section)
                .views(boardDto.getViews())
                .likeCount(boardDto.getLikesCount())
                .commentCount(boardDto.getCommentsCount())
                .state(boardDto.getState() == null ? SoftDeleteState.ACTIVE : boardDto.getState())
                .hiddenByReport(hiddenByReport)
                .build();
        Board savedBoard = boardRepository.save(board);
        imageService.syncBoardImages(savedBoard, sanitizedContents);
        return savedBoard;
    }

    @Transactional
    public void updateViews(Long id) {
        boardRepository.updateViews(id, SoftDeleteState.ACTIVE);
    }

    @Transactional
    public void delete(Long id) {
        Board board = boardRepository.findById(id).orElseThrow();
        imageService.deleteBoardImages(board);
        boardRepository.updateState(id, SoftDeleteState.DELETED);
    }

    @Transactional
    public void updateLikes(Long id) {
        boardRepository.updateLikes(id);
    }

    @Transactional
    public void updateCommentCounts(Long id) {
        Long commentCount = commentsRepository.countByBoardIdAndState(id, SoftDeleteState.ACTIVE);
        boardRepository.updateCommentCount(id, commentCount, SoftDeleteState.ACTIVE);
    }

    public int getFeaturedLikeThreshold() {
        return boardPolicyService.getFeaturedLikeThreshold();
    }

    public long getHiddenByReportCount() {
        return boardRepository.countByHiddenByReportTrue();
    }

    public List<Board> getPopularBoardsSince(LocalDateTime since, int limit) {
        int normalizedLimit = limit <= 0 ? 10 : Math.min(limit, 30);
        LocalDateTime baseline = since == null ? LocalDateTime.now().minusDays(7) : since;
        return boardRepository.findTopByStateAndRegTimeAfterOrderByPopularity(
                SoftDeleteState.ACTIVE,
                baseline,
                PageRequest.of(0, normalizedLimit));
    }

    public BoardThumbnailDisplayMode getThumbnailDisplayMode() {
        return boardPolicyService.getThumbnailDisplayMode();
    }

    private BlockedUserFilter resolveBlockedUserFilter(Collection<Long> blockedUserIds) {
        if (blockedUserIds == null || blockedUserIds.isEmpty()) {
            return new BlockedUserFilter(false, Set.of(-1L));
        }

        Set<Long> normalized = blockedUserIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (normalized.isEmpty()) {
            return new BlockedUserFilter(false, Set.of(-1L));
        }
        return new BlockedUserFilter(true, normalized);
    }

    private record BlockedUserFilter(boolean excludeBlocked, Collection<Long> blockedUserIds) {
    }

    private String sanitizeBoardContents(String rawContents) {
        String cleaned = Jsoup.clean(rawContents == null ? "" : rawContents, BOARD_CONTENT_SAFELIST);
        Document document = Jsoup.parseBodyFragment(cleaned);
        for (Element element : document.select("[style]")) {
            String sanitizedStyle = sanitizeAllowedInlineStyle(element.attr("style"));
            if (sanitizedStyle.isBlank()) {
                element.removeAttr("style");
                continue;
            }
            element.attr("style", sanitizedStyle);
        }
        return document.body().html();
    }

    private String sanitizeAllowedInlineStyle(String rawStyle) {
        if (rawStyle == null || rawStyle.isBlank()) {
            return "";
        }

        String[] declarations = rawStyle.split(";");
        List<String> safeDeclarations = new ArrayList<>();
        for (String declaration : declarations) {
            String[] pair = declaration.split(":", 2);
            if (pair.length != 2) {
                continue;
            }

            String property = pair[0].trim().toLowerCase(Locale.ROOT);
            String value = normalizeCssValue(pair[1]);
            if (property.equals("font-size")) {
                if (ALLOWED_FONT_SIZES.contains(value)) {
                    safeDeclarations.add("font-size: " + value);
                }
                continue;
            }

            if (property.equals("font-family")) {
                String normalizedValue = normalizeFontFamilyValue(value);
                String allowedValue = ALLOWED_FONT_FAMILIES.get(normalizedValue);
                if (allowedValue != null) {
                    safeDeclarations.add("font-family: " + allowedValue);
                }
            }
        }

        return String.join("; ", safeDeclarations);
    }

    private String normalizeCssValue(String cssValue) {
        if (cssValue == null) {
            return "";
        }
        String withoutImportant = cssValue.replaceAll("(?i)!important", "");
        return withoutImportant.trim().replaceAll("\\s+", " ");
    }

    private String normalizeFontFamilyValue(String fontFamily) {
        if (fontFamily == null) {
            return "";
        }
        return fontFamily.trim()
                .replace('"', '\'')
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    private static Map<String, String> createAllowedFontFamilies() {
        Map<String, String> allowed = new LinkedHashMap<>();
        allowed.put("'noto sans kr', 'noto sans', sans-serif", "'Noto Sans KR', 'Noto Sans', sans-serif");
        allowed.put("'noto serif kr', serif", "'Noto Serif KR', serif");
        allowed.put("'malgun gothic', '맑은 고딕', sans-serif", "'Malgun Gothic', '맑은 고딕', sans-serif");
        allowed.put("'nanum gothic', sans-serif", "'Nanum Gothic', sans-serif");
        allowed.put("'nanum myeongjo', serif", "'Nanum Myeongjo', serif");
        allowed.put("'gowun batang', serif", "'Gowun Batang', serif");
        allowed.put("'gothic a1', sans-serif", "'Gothic A1', sans-serif");
        allowed.put("'ibm plex sans kr', sans-serif", "'IBM Plex Sans KR', sans-serif");
        allowed.put("'arial', sans-serif", "'Arial', sans-serif");
        allowed.put("'georgia', serif", "'Georgia', serif");
        allowed.put("'courier new', monospace", "'Courier New', monospace");
        allowed.put("monospace", "monospace");
        allowed.put("'gungsuh', '궁서', serif", "'Gungsuh', '궁서', serif");
        return allowed;
    }

}
