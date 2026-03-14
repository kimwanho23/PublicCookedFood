package kwh.PublicCookedFood.board.service.support;

import kwh.PublicCookedFood.board.domain.Board;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class BoardThumbnailExtractor {

    public ThumbnailData extract(Iterable<Board> boards) {
        if (boards == null) {
            return ThumbnailData.empty();
        }

        Map<Long, String> thumbnailUrlByBoardId = new HashMap<>();
        Map<Long, Boolean> hasImageByBoardId = new HashMap<>();

        for (Board board : boards) {
            if (board == null || board.getId() == null) {
                continue;
            }
            Optional<String> thumbnailUrl = extractFirstImageUrl(board.getContents());
            if (thumbnailUrl.isPresent()) {
                thumbnailUrlByBoardId.put(board.getId(), thumbnailUrl.get());
                hasImageByBoardId.put(board.getId(), true);
                continue;
            }
            hasImageByBoardId.put(board.getId(), false);
        }

        return new ThumbnailData(thumbnailUrlByBoardId, hasImageByBoardId);
    }

    private Optional<String> extractFirstImageUrl(String htmlContent) {
        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            return Optional.empty();
        }
        return Jsoup.parse(htmlContent)
                .select("img[src]")
                .stream()
                .map(element -> element.attr("src"))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .filter(value -> !value.startsWith("data:"))
                .findFirst();
    }

    public static final class ThumbnailData {

        private static final ThumbnailData EMPTY = new ThumbnailData(
                Collections.<Long, String>emptyMap(),
                Collections.<Long, Boolean>emptyMap()
        );

        private final Map<Long, String> thumbnailUrlByBoardId;
        private final Map<Long, Boolean> hasImageByBoardId;

        public ThumbnailData(Map<Long, String> thumbnailUrlByBoardId,
                             Map<Long, Boolean> hasImageByBoardId) {
            this.thumbnailUrlByBoardId = toImmutableMap(thumbnailUrlByBoardId);
            this.hasImageByBoardId = toImmutableMap(hasImageByBoardId);
        }

        public static ThumbnailData empty() {
            return EMPTY;
        }

        public Map<Long, String> thumbnailUrlByBoardId() {
            return thumbnailUrlByBoardId;
        }

        public Map<Long, String> getThumbnailUrlByBoardId() {
            return thumbnailUrlByBoardId;
        }

        public Map<Long, Boolean> hasImageByBoardId() {
            return hasImageByBoardId;
        }

        public Map<Long, Boolean> getHasImageByBoardId() {
            return hasImageByBoardId;
        }

        private static <K, V> Map<K, V> toImmutableMap(Map<K, V> source) {
            if (source == null || source.isEmpty()) {
                return Collections.emptyMap();
            }
            return Collections.unmodifiableMap(new LinkedHashMap<>(source));
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof ThumbnailData)) {
                return false;
            }
            ThumbnailData that = (ThumbnailData) other;
            return Objects.equals(thumbnailUrlByBoardId, that.thumbnailUrlByBoardId)
                    && Objects.equals(hasImageByBoardId, that.hasImageByBoardId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(thumbnailUrlByBoardId, hasImageByBoardId);
        }
    }
}
