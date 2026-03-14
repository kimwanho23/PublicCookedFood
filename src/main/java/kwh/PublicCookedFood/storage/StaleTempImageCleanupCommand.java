package kwh.PublicCookedFood.storage;

import java.time.LocalDateTime;

public record StaleTempImageCleanupCommand(LocalDateTime cutoff, int batchSize) {
}
