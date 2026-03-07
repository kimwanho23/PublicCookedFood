package kwh.PublicCookedFood.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.ui.Model;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Paging
{
    public static <T> void addPagingAttributes(Model model, Page<T> pageList, Pageable pageable) {
        int totalPages = pageList == null ? 0 : pageList.getTotalPages();
        int requestedPage = pageable == null ? 0 : pageable.getPageNumber();
        int currentPage = 0;
        int startPage = 0;
        int endPage = 0;
        boolean hasPreviousGroup = false;
        boolean hasNextGroup = false;

        if (totalPages > 0) {
            currentPage = Math.max(0, Math.min(requestedPage, totalPages - 1));
            int currentGroup = currentPage / 10;
            startPage = currentGroup * 10;
            endPage = Math.min(startPage + 9, totalPages - 1);
            hasPreviousGroup = startPage > 0;
            hasNextGroup = endPage < totalPages - 1;
        }

        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("hasPreviousGroup", hasPreviousGroup);
        model.addAttribute("hasNextGroup", hasNextGroup);
    }

    public static String handleEmptyParamsRedirect(HttpServletRequest request, String keyword, String search) {
        // 현재 URL에서 빈 파라미터 제거
        String baseUrl = request.getRequestURL().toString();
        String queryString = request.getQueryString();

        Map<String, String> params = new LinkedHashMap<>();
        if (queryString != null) {
            Arrays.stream(queryString.split("&"))
                    .map(param -> param.split("=", 2))
                    .filter(parts -> parts.length == 2 && !parts[1].trim().isEmpty())
                    .forEach(parts -> params.put(parts[0], parts[1]));
        }

        if (keyword != null && keyword.trim().isEmpty()) {
            params.remove("keyword");
        }
        if (search != null && search.trim().isEmpty()) {
            params.remove("search");
        }

        String newQueryString = params.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));

        return "redirect:" + baseUrl + (newQueryString.isEmpty() ? "" : "?" + newQueryString);
    }
}
