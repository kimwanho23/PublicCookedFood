package kwh.PublicCookedFood.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(
                buildCache("recipeNationCategories", 8, Duration.ofHours(24)),
                buildCache("recipeTypeCategories", 8, Duration.ofHours(24)),
                buildCache("recipeIngredientCategories", 8, Duration.ofHours(24)),
                buildCache("recipeListPages", 256, Duration.ofHours(6)),
                buildCache("recipeFilteredPages", 512, Duration.ofMinutes(30))
        ));
        return cacheManager;
    }

    private CaffeineCache buildCache(String name, long maxSize, Duration ttl) {
        return new CaffeineCache(
                name,
                Caffeine.newBuilder()
                        .maximumSize(maxSize)
                        .expireAfterWrite(ttl)
                        .recordStats()
                        .build()
        );
    }
}
