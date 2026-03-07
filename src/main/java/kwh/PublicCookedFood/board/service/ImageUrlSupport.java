package kwh.PublicCookedFood.board.service;

import kwh.PublicCookedFood.storage.StorageCategory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

@Component
public class ImageUrlSupport {

    public Set<String> extractLocalImageUrls(String htmlContent) {
        if (htmlContent == null || htmlContent.isBlank()) {
            return Set.of();
        }

        Document document = Jsoup.parse(htmlContent);
        Set<String> imageUrls = new LinkedHashSet<>();
        for (Element imageTag : document.select("img[src]")) {
            String src = imageTag.attr("src");
            normalizeLocalImageUrl(src).ifPresent(imageUrls::add);
        }
        return imageUrls;
    }

    public Optional<String> normalizeLocalImageUrl(String src) {
        if (src == null || src.isBlank()) {
            return Optional.empty();
        }

        String trimmedSrc = src.trim();
        if (trimmedSrc.startsWith(StorageCategory.IMAGE.getUrlPrefix())) {
            return Optional.of(trimmedSrc);
        }

        if (!trimmedSrc.startsWith("http://") && !trimmedSrc.startsWith("https://")) {
            return Optional.empty();
        }

        try {
            String path = URI.create(trimmedSrc).getPath();
            if (path != null && path.startsWith(StorageCategory.IMAGE.getUrlPrefix())) {
                return Optional.of(path);
            }
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
        return Optional.empty();
    }
}
