package kwh.PublicCookedFood.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.image.temp-cleanup")
public record ImageTempCleanupProperties(
        @NotNull Boolean enabled,
        @NotNull @Min(1) Long ttlHours,
        @NotNull @Min(1) Integer batchSize,
        @NotNull @Min(1) Integer maxBatchesPerRun,
        @NotBlank String cron) {
}
