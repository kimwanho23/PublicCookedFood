package kwh.PublicCookedFood.config.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.recipe-open-api")
public record RecipeOpenApiProperties(
        @NotBlank String baseUrl,
        String apiKey,
        @NotBlank String crseCode,
        @NotBlank String infoCode,
        @NotBlank String irdntCode,
        String crseEndpoint,
        String infoEndpoint,
        String irdntEndpoint) {
}
