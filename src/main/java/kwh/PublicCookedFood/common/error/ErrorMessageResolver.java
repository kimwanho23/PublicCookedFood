package kwh.PublicCookedFood.common.error;

public final class ErrorMessageResolver {

    private ErrorMessageResolver() {
    }

    public static String resolve(Throwable throwable, String fallbackMessage) {
        if (throwable == null) {
            return resolve((String) null, fallbackMessage);
        }
        return resolve(throwable.getMessage(), fallbackMessage);
    }

    public static String resolve(String candidate, String fallbackMessage) {
        if (candidate == null || candidate.isBlank()) {
            return fallbackMessage;
        }
        return candidate;
    }
}
