package kwh.PublicCookedFood.config.properties;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.image.processing")
public record ImageProcessingProperties(
        @NotNull Boolean enabled,
        @NotNull @Min(1) Integer maxWidth,
        @NotNull @Min(1) Integer maxHeight,
        @NotNull @DecimalMin("0.1") @DecimalMax("1.0") Float jpegQuality) {
}
