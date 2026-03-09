package kwh.PublicCookedFood.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.account")
public record InitialAdminProperties(String initialAdminEmail) {
}
