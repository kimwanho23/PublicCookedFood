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

    @Value("${recipe-api-key}")
    private String apiKey;

    @Value("${VIEW_TN_RECIPE_CRSE}")
    private String crse;

    @Value("${VIEW_TN_RECIPE_INFO}")
    private String info;

    @Value("${VIEW_TN_RECIPE_IRDNT}")
    private String irdnt;


    public String getRecipe_CRSE(int startRow, int endRow) {
        return restClient.get()
                .uri(crse + "/" +startRow + "/" +endRow)
                .retrieve()
                .body(String.class); //String 형태로 Json 데이터를 받아옴
    }

    public String getRecipe_INFO(int startRow, int endRow) {
        return restClient.get()
                .uri(info + "/" +startRow + "/" +endRow)
                .retrieve()
                .body(String.class);
    }

    public String getRecipe_IRDNT(int startRow, int endRow) {
        return restClient.get()
                .uri(irdnt + "/" +startRow + "/" +endRow)
                .retrieve()
                .body(String.class);
    }


}
