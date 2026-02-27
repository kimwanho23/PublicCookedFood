package kwh.PublicCookedFood.config;

import kwh.PublicCookedFood.config.properties.StorageProperties;
import kwh.PublicCookedFood.config.oauth2.LoginUserArgumentResolver;
import kwh.PublicCookedFood.storage.StorageCategory;
import kwh.PublicCookedFood.storage.StoragePathUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.util.List;

@RequiredArgsConstructor
@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final LoginUserArgumentResolver loginUserArgumentResolver;
    private final StorageProperties storageProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = StoragePathUtils.resolveUploadPath(storageProperties.dir());
        Path imagePath = uploadPath.resolve(StorageCategory.IMAGE.getDirectoryName()).normalize();
        Path filePath = uploadPath.resolve(StorageCategory.ATTACHMENT.getDirectoryName()).normalize();

        registry.addResourceHandler("/images/**")
                .addResourceLocations(
                        toResourceLocation(imagePath),
                        toResourceLocation(uploadPath) // 기존 경로 호환
                );

        registry.addResourceHandler("/files/**")
                .addResourceLocations(toResourceLocation(filePath));
    }

    private String toResourceLocation(Path path) {
        String resourceLocation = path.toUri().toString();
        return resourceLocation.endsWith("/") ? resourceLocation : resourceLocation + "/";
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolver) {
        argumentResolver.add(loginUserArgumentResolver);
    }
}
