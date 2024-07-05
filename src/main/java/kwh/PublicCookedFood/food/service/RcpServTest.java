package kwh.PublicCookedFood.food.service;

import kwh.PublicCookedFood.food.entity.Recipe_CRSE;
import kwh.PublicCookedFood.food.repository.Recipe_CRSE_Repository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class RcpServTest {

    private final Recipe_CRSE_Repository recipe_CRSE_Repository;

    private final RestClient restClient;

    @Value("${recipe-api-key}")
    private String apiKey;

    @Value("${VIEW_TN_RECIPE_CRSE}")
    private String crse;

    @Value("${VIEW_TN_RECIPE_INFO}")
    private String info;

    @Value("${VIEW_TN_RECIPE_IRDNT}")
    private String irdnt;

    public Recipe_CRSE recipe_CRSE_Info() {

        return restClient.get()
                .uri(crse)
                .retrieve()
                .body(Recipe_CRSE.class);
    }

/*    public String recipe_CRSE_Save(){
    }*/


}
