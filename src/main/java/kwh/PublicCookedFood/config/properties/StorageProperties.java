package kwh.PublicCookedFood.config.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "file")
public record StorageProperties(@NotBlank String dir) {
}
