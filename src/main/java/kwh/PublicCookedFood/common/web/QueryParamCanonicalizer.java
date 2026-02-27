package kwh.PublicCookedFood.common.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;

public final class QueryParamCanonicalizer {

    private QueryParamCanonicalizer() {
    }

    public static String buildRedirectIfHasEmptyValues(HttpServletRequest request, String basePath) {
        if (request == null || basePath == null || basePath.isBlank()) {
            return null;
        }

        boolean hasEmptyParam = request.getParameterMap().values().stream()
                .flatMap(Arrays::stream)
                .anyMatch(value -> value == null || value.trim().isEmpty());
        if (!hasEmptyParam) {
            return null;
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(basePath);
        request.getParameterMap().forEach((key, values) -> {
            if (values == null) {
                return;
            }
            for (String value : values) {
                if (value != null && !value.trim().isEmpty()) {
                    builder.queryParam(key, value.trim());
                }
            }
        });
        return "redirect:" + builder.build().encode().toUriString();
    }
}
