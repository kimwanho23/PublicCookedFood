package kwh.PublicCookedFood.config.properties;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.image")
public record ImageSchemaProperties(@NotNull Boolean schemaAutoMigrate) {
}
