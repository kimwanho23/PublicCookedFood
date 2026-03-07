package kwh.PublicCookedFood.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class PageConfig implements WebMvcConfigurer {

    private static final int DEFAULT_PAGE_SIZE = 15;
    private static final int MAX_PAGE_SIZE = 50;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        PageableHandlerMethodArgumentResolver pageableHandlerMethodArgumentResolver = new PageableHandlerMethodArgumentResolver();
        pageableHandlerMethodArgumentResolver.setMaxPageSize(MAX_PAGE_SIZE);
        pageableHandlerMethodArgumentResolver.setFallbackPageable(PageRequest.of(0, DEFAULT_PAGE_SIZE));
        pageableHandlerMethodArgumentResolver.setOneIndexedParameters(true);
        resolvers.add(pageableHandlerMethodArgumentResolver);
    }
}
