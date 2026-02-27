package kwh.PublicCookedFood.config.properties;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.ai.openai")
public record OpenAiProperties(
        @NotNull Boolean enabled,
        @NotBlank String baseUrl,
        @NotBlank String model,
        String apiKey,
        @NotNull @DecimalMin("0.0") @DecimalMax("2.0") Double temperature) {
}
