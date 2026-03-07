package kwh.PublicCookedFood.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableScheduling
@EnableAsync
public class SchedulingConfig {

    @Value("${app.popular.recipe.score.async.core-pool-size:1}")
    private int recipeScoreExecutorCorePoolSize;

    @Value("${app.popular.recipe.score.async.max-pool-size:2}")
    private int recipeScoreExecutorMaxPoolSize;

    @Value("${app.popular.recipe.score.async.queue-capacity:200}")
    private int recipeScoreExecutorQueueCapacity;

    @Bean(name = "recipeScoreEventExecutor")
    public Executor recipeScoreEventExecutor() {
        return createExecutor(
                "recipe-score-",
                recipeScoreExecutorCorePoolSize,
                recipeScoreExecutorMaxPoolSize,
                recipeScoreExecutorQueueCapacity
        );
    }

    private Executor createExecutor(String threadNamePrefix,
                                    int corePoolSize,
                                    int maxPoolSize,
                                    int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setCorePoolSize(Math.max(1, corePoolSize));
        executor.setMaxPoolSize(Math.max(1, maxPoolSize));
        executor.setQueueCapacity(Math.max(1, queueCapacity));
        executor.initialize();
        return executor;
    }
}
