package kwh.PublicCookedFood.recipeSaveLogic.facade;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kwh.PublicCookedFood.config.properties.RecipeOpenApiProperties;
import kwh.PublicCookedFood.food.dto.response.recipe_crse.Recipe_CRSE_ResponseDto;
import kwh.PublicCookedFood.food.dto.response.recipe_info.Recipe_INFO_ResponseDto;
import kwh.PublicCookedFood.food.dto.response.recipe_irdnt.Recipe_IRDNT_ResponseDto;
import kwh.PublicCookedFood.food.entity.Recipe_CRSE;
import kwh.PublicCookedFood.food.entity.Recipe_INFO;
import kwh.PublicCookedFood.food.entity.Recipe_IRDNT;
import kwh.PublicCookedFood.food.repository.Recipe_CRSE_Repository;
import kwh.PublicCookedFood.food.repository.Recipe_INFO_Repository;
import kwh.PublicCookedFood.food.repository.Recipe_IRDNT_Repository;
import kwh.PublicCookedFood.recipeSaveLogic.RecipeOpenApiClient;
import kwh.PublicCookedFood.recipeSaveLogic.RecipeImportException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecipeImportFacade {

    private static final int PAGE_SIZE = 1_000;

    private final Recipe_CRSE_Repository crseRepository;
    private final Recipe_INFO_Repository infoRepository;
    private final Recipe_IRDNT_Repository irdntRepository;
    private final RecipeOpenApiClient recipeOpenApiClient;
    private final RecipeOpenApiProperties recipeOpenApiProperties;
    private final ObjectMapper objectMapper;

    public List<Recipe_CRSE_ResponseDto> importCourses() {
        return importDataset(
                this::crseCount,
                recipeOpenApiClient::getRecipeCrse,
                recipeOpenApiProperties.crseCode(),
                Recipe_CRSE_ResponseDto.class,
                this::toCrseEntity,
                crseRepository::saveAll
        );
    }

    public List<Recipe_INFO_ResponseDto> importInfos() {
        return importDataset(
                this::infoCount,
                recipeOpenApiClient::getRecipeInfo,
                recipeOpenApiProperties.infoCode(),
                Recipe_INFO_ResponseDto.class,
                this::toInfoEntity,
                infoRepository::saveAll
        );
    }

    public List<Recipe_IRDNT_ResponseDto> importIngredients() {
        return importDataset(
                this::irdntCount,
                recipeOpenApiClient::getRecipeIrdnt,
                recipeOpenApiProperties.irdntCode(),
                Recipe_IRDNT_ResponseDto.class,
                this::toIrdntEntity,
                irdntRepository::saveAll
        );
    }

    private <D, E> List<D> importDataset(CountSupplier countSupplier,
                                         DatasetFetcher datasetFetcher,
                                         String datasetKey,
                                         Class<D> dtoClass,
                                         Function<D, E> entityMapper,
                                         Consumer<List<E>> entitySaver) {
        int startRow = 1;
        int endRow = PAGE_SIZE;
        List<D> allRows = new ArrayList<>();

        try {
            int totalCount = countSupplier.get();
            while (startRow <= totalCount) {
                String data = datasetFetcher.fetch(startRow, endRow);
                JsonNode rowNode = objectMapper.readTree(data).path(datasetKey).path("row");
                List<D> rows = objectMapper.convertValue(
                        rowNode,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, dtoClass)
                );

                allRows.addAll(rows);
                startRow += PAGE_SIZE;
                endRow += PAGE_SIZE;
            }

            List<E> entities = allRows.stream()
                    .map(entityMapper)
                    .toList();
            entitySaver.accept(entities);
            log.info("recipe import dataset={} count={}", datasetKey, entities.size());
            return allRows;
        } catch (Exception e) {
            log.error("recipe import failed dataset={}", datasetKey, e);
            throw new RecipeImportException("레시피 데이터 동기화에 실패했습니다. dataset=" + datasetKey, e);
        }
    }

    private int crseCount() throws JsonProcessingException {
        String initialData = recipeOpenApiClient.getRecipeCrse(1, 1);
        return extractTotalCount(initialData, recipeOpenApiProperties.crseCode());
    }

    private int infoCount() throws JsonProcessingException {
        String initialData = recipeOpenApiClient.getRecipeInfo(1, 1);
        return extractTotalCount(initialData, recipeOpenApiProperties.infoCode());
    }

    private int irdntCount() throws JsonProcessingException {
        String initialData = recipeOpenApiClient.getRecipeIrdnt(1, 1);
        return extractTotalCount(initialData, recipeOpenApiProperties.irdntCode());
    }

    private int extractTotalCount(String payload, String datasetKey) throws JsonProcessingException {
        JsonNode rootNode = objectMapper.readTree(payload);
        return rootNode.path(datasetKey).path("totalCnt").asInt();
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

    @FunctionalInterface
    private interface CountSupplier {
        int get() throws Exception;
    }

    @FunctionalInterface
    private interface DatasetFetcher {
        String fetch(int startRow, int endRow) throws Exception;
    }
}
