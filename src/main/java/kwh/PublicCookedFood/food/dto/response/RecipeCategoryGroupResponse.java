package kwh.PublicCookedFood.food.dto.response;

import java.util.List;

public class RecipeCategoryGroupResponse {

    private final String title;

    private final String param;

    private final List<String> items;

    public RecipeCategoryGroupResponse(String title, String param, List<String> items) {
        this.title = title;
        this.param = param;
        this.items = items;
    }

    public String getTitle() {
        return title;
    }

    public String getParam() {
        return param;
    }

    public List<String> getItems() {
        return items;
    }
}
