package kwh.PublicCookedFood.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;

@Configuration
@EnableScheduling
@EnableAsync
public class SchedulingConfig {

    @Bean(name = "recipeAiExecutor")
    public Executor recipeAiExecutor() {
        return createExecutor("recipe-ai-");
    }

    private Executor createExecutor(String threadNamePrefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(200);
        executor.initialize();
        return executor;
    }
}
