package kwh.PublicCookedFood.recipeSaveLogic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import kwh.PublicCookedFood.food.dto.response.recipe_crse.Recipe_CRSE_ResponseDto;
import kwh.PublicCookedFood.food.dto.response.recipe_info.Recipe_INFO_ResponseDto;
import kwh.PublicCookedFood.food.dto.response.recipe_irdnt.Recipe_IRDNT_ResponseDto;
import kwh.PublicCookedFood.food.entity.Recipe_CRSE;
import kwh.PublicCookedFood.food.entity.Recipe_INFO;
import kwh.PublicCookedFood.food.entity.Recipe_IRDNT;
import kwh.PublicCookedFood.food.repository.Recipe_CRSE_Repository;
import kwh.PublicCookedFood.food.repository.Recipe_INFO_Repository;
import kwh.PublicCookedFood.food.repository.Recipe_IRDNT_Repository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;


@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/admin/recipes/import")
@Tag(name = "Admin Recipe Import API")
public class RecipeSaveController {

    private final Recipe_CRSE_Repository crseRepository;

    private final Recipe_INFO_Repository infoRepository;

    private final Recipe_IRDNT_Repository irdntRepository;

    private final RcpServTest rcpServTest;

    @Value("${IRDNT_URL}")
    private String IRDNT_URL;

    @Value("${INFO_URL}")
    private String INFO_URL;

    @Value("${CRSE_URL}")
    private String CRSE_URL;

    @PostMapping("/courses")
    public ResponseEntity<List<Recipe_CRSE_ResponseDto>> getRecipeCRSE() throws JsonProcessingException {
        int startRow = 1;
        int endRow = 1000;
        int totalCount = crse_Count();

        ObjectMapper objectMapper = new ObjectMapper();
        List<Recipe_CRSE_ResponseDto> allRows = new ArrayList<>();

        try {
            while (startRow <= totalCount) {
                String data = rcpServTest.getRecipe_CRSE(startRow, endRow);
                JsonNode rootNode = objectMapper.readTree(data);
                JsonNode gridDataNode = rootNode.path(CRSE_URL);

                // "row" 배열을 추출
                JsonNode rowNode = gridDataNode.path("row");
                // 이를 리스트에 담아 반환
                List<Recipe_CRSE_ResponseDto> rows = objectMapper
                        .convertValue(rowNode, objectMapper.getTypeFactory().constructCollectionType(List.class,
                                Recipe_CRSE_ResponseDto.class));

                allRows.addAll(rows);
                // 다음 페이지 설정
                startRow += 1000;
                endRow += 1000;
            }

            List<Recipe_CRSE> entities = allRows.stream()
                    .map(this::toCrseEntity)
                    .toList();
            log.info(String.valueOf(entities.size()));
            crseRepository.saveAll(entities);

            return ResponseEntity.ok(allRows);
        } catch (JsonProcessingException e) {
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/infos")
    public ResponseEntity<List<Recipe_INFO_ResponseDto>> getRecipeINFO() throws JsonProcessingException {
        int startRow = 1;
        int endRow = 1000;
        int totalCount = info_Count();

        ObjectMapper objectMapper = new ObjectMapper();
        List<Recipe_INFO_ResponseDto> allRows = new ArrayList<>();

        try {
            while (startRow <= totalCount) {
                String data = rcpServTest.getRecipe_INFO(startRow, endRow);
                JsonNode rootNode = objectMapper.readTree(data);
                JsonNode gridDataNode = rootNode.path(INFO_URL);

                // "row" 배열을 추출
                JsonNode rowNode = gridDataNode.path("row");
                // 이를 리스트에 담아 반환
                List<Recipe_INFO_ResponseDto> rows = objectMapper
                        .convertValue(rowNode, objectMapper.getTypeFactory().constructCollectionType(List.class,
                                Recipe_INFO_ResponseDto.class));

                allRows.addAll(rows);
                // 다음 페이지 설정
                startRow += 1000;
                endRow += 1000;
            }

            List<Recipe_INFO> entities = allRows.stream()
                    .map(this::toInfoEntity)
                    .toList();
            log.info(String.valueOf(entities.size()));
            infoRepository.saveAll(entities);

            return ResponseEntity.ok(allRows);
        } catch (JsonProcessingException e) {
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/ingredients")
    public ResponseEntity<List<Recipe_IRDNT_ResponseDto>> getRecipeIRDNT() throws JsonProcessingException {
        int startRow = 1;
        int endRow = 1000; //1부터 ~ 1000까지 읽는다
        int totalCount = irdnt_Count();

        ObjectMapper objectMapper = new ObjectMapper();
        List<Recipe_IRDNT_ResponseDto> allRows = new ArrayList<>();

        try {
            while (startRow <= totalCount) {
                String data = rcpServTest.getRecipe_IRDNT(startRow, endRow);
                JsonNode rootNode = objectMapper.readTree(data);
                JsonNode gridDataNode = rootNode.path(IRDNT_URL);

                // "row" 배열을 추출
                JsonNode rowNode = gridDataNode.path("row");
                // 이를 리스트에 담아 반환
                List<Recipe_IRDNT_ResponseDto> rows = objectMapper
                        .convertValue(rowNode, objectMapper.getTypeFactory().constructCollectionType(List.class,
                                Recipe_IRDNT_ResponseDto.class));

                allRows.addAll(rows);
                // 다음 페이지 설정
                startRow += 1000; //1~부터 1000까지 읽고, 1001부터 ~ 2000까지 읽기 위해 row 증가
                endRow += 1000;
            }

            List<Recipe_IRDNT> entities = allRows.stream()
                    .map(this::toIrdntEntity)
                    .toList();
            log.info(String.valueOf(entities.size()));
            irdntRepository.saveAll(entities);
            return ResponseEntity.ok(allRows);
        } catch (JsonProcessingException e) {
            return ResponseEntity.status(500).build();
        }
    }

    private int crse_Count() throws JsonProcessingException {
        String initialData = rcpServTest.getRecipe_CRSE(1,1);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(initialData);
        return rootNode.path(CRSE_URL).path("totalCnt").asInt();
    }

    private int info_Count() throws JsonProcessingException {
        String initialData = rcpServTest.getRecipe_INFO(1,1);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(initialData);
        return rootNode.path(INFO_URL).path("totalCnt").asInt();
    }

    private int irdnt_Count() throws JsonProcessingException {
        String initialData = rcpServTest.getRecipe_IRDNT(1,1);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(initialData);
        return rootNode.path(IRDNT_URL).path("totalCnt").asInt();
    }

    private Recipe_CRSE toCrseEntity(Recipe_CRSE_ResponseDto dto) {
        return Recipe_CRSE.builder()
                .rowNUM(dto.getRowNUM())
                .recipeID(dto.getRecipeID())
                .cookingNO(dto.getCookingNO())
                .cookingDC(dto.getCookingDC())
                .stepTIP(dto.getStepTIP())
                .imgURL(dto.getImgURL())
                .build();
    }

    private Recipe_INFO toInfoEntity(Recipe_INFO_ResponseDto dto) {
        return Recipe_INFO.builder()
                .rowNUM(dto.getRowNUM())
                .recipeID(dto.getRecipeID())
                .recipeNMKO(dto.getRecipeNMKO())
                .sumry(dto.getSumry())
                .nationCODE(dto.getNationCODE())
                .nationNM(dto.getNationNM())
                .tyCODE(dto.getTyCODE())
                .tyNM(dto.getTyNM())
                .cookingTIME(dto.getCookingTIME())
                .calorie(dto.getCalorie())
                .qnt(dto.getQnt())
                .levelNM(dto.getLevelNM())
                .irdntCODE(dto.getIrdntCODE())
                .pcNM(dto.getPcNM())
                .imgURL(dto.getImgURL())
                .build();
    }

    private Recipe_IRDNT toIrdntEntity(Recipe_IRDNT_ResponseDto dto) {
        return Recipe_IRDNT.builder()
                .rowNUM(dto.getRowNUM())
                .recipeID(dto.getRecipeID())
                .irdntSN(dto.getIrdntSN())
                .irdntNM(dto.getIrdntNM())
                .irdntCPCTY(dto.getIrdntCPCTY())
                .irdntTYCODE(dto.getIrdntTYCODE())
                .irdntTYNM(dto.getIrdntTYNM())
                .build();
    }

}
