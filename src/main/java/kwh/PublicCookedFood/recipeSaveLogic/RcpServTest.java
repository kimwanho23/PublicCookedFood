package kwh.PublicCookedFood.recipeSaveLogic;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class RcpServTest {

    private final RestClient restClient;

    @Value("${RECIPE_API_BASE_URL:http://211.237.50.150:7080/openapi}")
    private String recipeApiBaseUrl;

    @Value("${RECIPE_API_KEY:}")
    private String apiKey;

    @Value("${CRSE_URL:Grid_20150827000000000228_1}")
    private String crseUrl;

    @Value("${INFO_URL:Grid_20150827000000000226_1}")
    private String infoUrl;

    @Value("${IRDNT_URL:Grid_20150827000000000227_1}")
    private String irdntUrl;

    @Value("${VIEW_TN_RECIPE_CRSE:}")
    private String crse;

    @Value("${VIEW_TN_RECIPE_INFO:}")
    private String info;

    @Value("${VIEW_TN_RECIPE_IRDNT:}")
    private String irdnt;


    public String getRecipe_CRSE(int startRow, int endRow) {
        String endpoint = resolveEndpoint(crse, crseUrl);
        return restClient.get()
                .uri(endpoint + "/" +startRow + "/" +endRow)
                .retrieve()
                .body(String.class); //String 형태로 Json 데이터를 받아옴
    }

    public String getRecipe_INFO(int startRow, int endRow) {
        String endpoint = resolveEndpoint(info, infoUrl);
        return restClient.get()
                .uri(endpoint + "/" +startRow + "/" +endRow)
                .retrieve()
                .body(String.class);
    }

    public String getRecipe_IRDNT(int startRow, int endRow) {
        String endpoint = resolveEndpoint(irdnt, irdntUrl);
        return restClient.get()
                .uri(endpoint + "/" +startRow + "/" +endRow)
                .retrieve()
                .body(String.class);
    }

    private String resolveEndpoint(String endpointOverride, String datasetCode) {
        if (endpointOverride != null && !endpointOverride.isBlank()) {
            return endpointOverride;
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("RECIPE_API_KEY 또는 VIEW_TN_RECIPE_* 설정이 필요합니다.");
        }

        String normalizedBaseUrl = recipeApiBaseUrl.endsWith("/")
                ? recipeApiBaseUrl.substring(0, recipeApiBaseUrl.length() - 1)
                : recipeApiBaseUrl;

        return normalizedBaseUrl + "/" + apiKey + "/json/" + datasetCode;
    }

}
