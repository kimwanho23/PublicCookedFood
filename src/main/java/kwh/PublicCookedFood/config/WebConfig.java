package kwh.PublicCookedFood.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@RequiredArgsConstructor
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // /uploads/** 경로로 들어오는 요청을 C:/cookboardImageFile/ 디렉토리와 매핑
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:///C:/cookboardImageFile/");
    }
}
