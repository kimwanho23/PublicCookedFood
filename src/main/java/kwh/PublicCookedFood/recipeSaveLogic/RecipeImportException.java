package kwh.PublicCookedFood.recipeSaveLogic;

public class RecipeImportException extends RuntimeException {

    public RecipeImportException(String message) {
        super(message);
    }

    public RecipeImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
