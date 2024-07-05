package kwh.PublicCookedFood.food.controller;

import kwh.PublicCookedFood.food.entity.Recipe_CRSE;
import kwh.PublicCookedFood.food.service.RcpServTest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@RestController
@Slf4j
@RequiredArgsConstructor
public class RecipeController {

    private final RcpServTest rcpServTest;

    @Value("${recipe-api-key}")
    private String apiKey;

    @Value("${VIEW_TN_RECIPE_IRDNT}")
    private String VIEW_TN_RECIPE_IRDNT;

    @Value("${VIEW_TN_RECIPE_INFO}")
    private String VIEW_TN_RECIPE_INFO;

    @Value("${VIEW_TN_RECIPE_CRSE}")
    private String VIEW_TN_RECIPE_CRSE;


    @GetMapping("/sample")
    public String call_Sample_View_Tn_Recipe_IRDNT() throws IOException {
        StringBuilder result = new StringBuilder();
        String urlStr = "http://211.237.50.150:7080/" +
                "openapi/" +
                "sample/" +
                "json/" +
                "Grid_20150827000000000226_1/" +
                "1/" +
                "3";
        URL url = new URL(urlStr);

        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

        urlConnection.setRequestMethod("GET");

        BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

        String returnLine;

        while ((returnLine = br.readLine()) != null) {
            result.append(returnLine).append("\n\r");
        }

        urlConnection.disconnect();

        return result.toString();
    }

    @GetMapping(value = "/json")
    public Recipe_CRSE getRecipeCRSE(){
        return rcpServTest.recipe_CRSE_Info();


/*        String data = rcpServTest.recipe_CRSE_Info();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode rootNode = objectMapper.readTree(data);
            JsonNode gridDataNode = rootNode.path("Grid_20150827000000000228_1");

            // "row" 배열을 추출
            JsonNode rowNode = gridDataNode.path("row");

            // 이를 리스트에 담아 반환
            List<Recipe_CRSE_ResponseDto> rows = objectMapper
                    .convertValue(rowNode, objectMapper.getTypeFactory().constructCollectionType(List.class,
                            Recipe_CRSE_ResponseDto.class));
            for (Recipe_CRSE_ResponseDto row : rows) {
                log.info(row.toString());
            }
            return ResponseEntity.ok(rows);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }*/
    }


}
