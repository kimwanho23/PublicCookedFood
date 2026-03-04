package kwh.PublicCookedFood.food.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kwh.PublicCookedFood.config.properties.OpenAiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecipeAiLlmService {

    private static final int DEFAULT_MAX_COMPLETION_TOKENS = 500;
    private static final String DEFAULT_SYSTEM_PROMPT = """
            너는 레시피 도우미다.
            반드시 제공된 레시피 문맥 안에서만 답하고, 문맥에 없는 정보는 추측하지 마라.
            답변은 한국어로, 핵심만 3~6문장으로 간결하게 작성해라.
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final OpenAiProperties openAiProperties;

    public Optional<String> generateAnswer(String recipeDocument, String question) {
        boolean openAiEnabled = Boolean.TRUE.equals(openAiProperties.enabled());
        String openAiApiKey = openAiProperties.apiKey();
        if (!openAiEnabled || openAiApiKey == null || openAiApiKey.isBlank()) {
            return Optional.empty();
        }

        String normalizedQuestion = normalizeText(question, 500);
        String normalizedDocument = normalizeText(recipeDocument, 4500);
        if (normalizedQuestion == null || normalizedDocument == null) {
            return Optional.empty();
        }

        String openAiModel = openAiProperties.model();
        double temperature = openAiProperties.temperature();
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", openAiModel);
        requestBody.put("temperature", temperature);
        applyCompletionTokenField(requestBody, openAiModel);
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", DEFAULT_SYSTEM_PROMPT),
                Map.of("role", "user", "content", buildUserPrompt(normalizedDocument, normalizedQuestion))
        ));

        try {
            String responseBody = restClient.post()
                    .uri(resolveChatCompletionsUri())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + openAiApiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(responseBody);
            String content = root.path("choices").path(0).path("message").path("content").asText();
            if (content == null || content.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(content.trim());
        } catch (Exception e) {
            log.warn("OpenAI answer generation failed. Falling back to rule-based answer. reason={}", e.getMessage());
            return Optional.empty();
        }
    }

    private String buildUserPrompt(String recipeDocument, String question) {
        return "[레시피 문맥]\n"
                + recipeDocument
                + "\n\n[질문]\n"
                + question
                + "\n\n문맥 기반으로만 답변해줘.";
    }

    private String resolveChatCompletionsUri() {
        String openAiBaseUrl = openAiProperties.baseUrl();
        String normalized = openAiBaseUrl == null ? "" : openAiBaseUrl.trim();
        if (normalized.isBlank()) {
            normalized = "https://api.openai.com/v1";
        }
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.endsWith("/chat/completions")) {
            return normalized;
        }
        normalized = ensureApiVersionPath(normalized);
        return normalized + "/chat/completions";
    }

    private String ensureApiVersionPath(String baseUrl) {
        try {
            URI uri = URI.create(baseUrl);
            String host = uri.getHost();
            String path = uri.getPath();
            boolean openAiHost = host != null && host.equalsIgnoreCase("api.openai.com");
            boolean hasVersionPath = path != null && path.toLowerCase(Locale.ROOT).matches(".*/v\\d+/?$");
            if (openAiHost && !hasVersionPath) {
                return baseUrl + "/v1";
            }
            return baseUrl;
        } catch (IllegalArgumentException ignored) {
            return baseUrl;
        }
    }

    private void applyCompletionTokenField(Map<String, Object> requestBody, String model) {
        String normalizedModel = model == null ? "" : model.trim().toLowerCase(Locale.ROOT);
        if (normalizedModel.startsWith("gpt-5")) {
            requestBody.put("max_completion_tokens", DEFAULT_MAX_COMPLETION_TOKENS);
            return;
        }
        requestBody.put("max_tokens", DEFAULT_MAX_COMPLETION_TOKENS);
    }

    private String normalizeText(String raw, int maxLength) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }
}
