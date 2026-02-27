package kwh.PublicCookedFood.recipeSaveLogic;

import kwh.PublicCookedFood.config.properties.RecipeOpenApiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class RecipeOpenApiClient {

    private final RestClient restClient;
    private final RecipeOpenApiProperties recipeOpenApiProperties;


    public String getRecipeCrse(int startRow, int endRow) {
        String endpoint = resolveEndpoint(
                recipeOpenApiProperties.crseEndpoint(),
                recipeOpenApiProperties.crseCode()
        );
        return restClient.get()
                .uri(endpoint + "/" + startRow + "/" + endRow)
                .retrieve()
                .body(String.class); //String 형태로 Json 데이터를 받아옴
    }

    public String getRecipeInfo(int startRow, int endRow) {
        String endpoint = resolveEndpoint(
                recipeOpenApiProperties.infoEndpoint(),
                recipeOpenApiProperties.infoCode()
        );
        return restClient.get()
                .uri(endpoint + "/" + startRow + "/" + endRow)
                .retrieve()
                .body(String.class);
    }

    public String getRecipeIrdnt(int startRow, int endRow) {
        String endpoint = resolveEndpoint(
                recipeOpenApiProperties.irdntEndpoint(),
                recipeOpenApiProperties.irdntCode()
        );
        return restClient.get()
                .uri(endpoint + "/" + startRow + "/" + endRow)
                .retrieve()
                .body(String.class);
    }

    private String resolveEndpoint(String endpointOverride, String datasetCode) {
        if (endpointOverride != null && !endpointOverride.isBlank()) {
            return endpointOverride;
        }
        String apiKey = recipeOpenApiProperties.apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("app.recipe-open-api.api-key 또는 app.recipe-open-api.*-endpoint 설정이 필요합니다.");
        }

        String recipeApiBaseUrl = recipeOpenApiProperties.baseUrl();
        String normalizedBaseUrl = recipeApiBaseUrl.endsWith("/")
                ? recipeApiBaseUrl.substring(0, recipeApiBaseUrl.length() - 1)
                : recipeApiBaseUrl;

        return normalizedBaseUrl + "/" + apiKey + "/json/" + datasetCode;
    }

}
