package kwh.PublicCookedFood.food.dto.recipe_info;

import lombok.*;
import org.springframework.beans.factory.annotation.Value;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class Recipe_INFO_RequestDto { //레시피 기본 정보

    @Value("${recipe-api-key}")
    private String API_KEY;	//기본	sample	발급받은 API_KEY

    private String TYPE;	//기본	xml	요청파일 타입 xml, json

    private String API_URL;	//기본	Grid_20150827000000000228_1	OpenAPI 서비스 URL

    private String START_INDEX;	//기본	1	요청시작위치

    private String END_INDEX;	//기본	5	요청종료위치

    @Builder
    public Recipe_INFO_RequestDto(String API_KEY, String TYPE, String API_URL, String START_INDEX, String END_INDEX) {
        this.API_KEY = API_KEY;
        this.TYPE = TYPE;
        this.API_URL = API_URL;
        this.START_INDEX = START_INDEX;
        this.END_INDEX = END_INDEX;
    }
}
