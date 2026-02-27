package kwh.PublicCookedFood.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.notification.sse")
public record NotificationSseProperties(
        @NotNull Boolean enabled,
        @NotNull @Min(1) Long heartbeatIntervalMs,
        @NotNull @Min(1) Long metricsLogIntervalMs) {
}
