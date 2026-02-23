package kwh.PublicCookedFood.config;

import kwh.PublicCookedFood.config.oauth2.LoginUserArgumentResolver;
import kwh.PublicCookedFood.storage.StorageCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${file.dir}")
    private String fileDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = resolveUploadPath(fileDir);
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

    private Path resolveUploadPath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            throw new IllegalArgumentException("file.dir 값이 비어 있습니다.");
        }

        String normalized = rawPath.trim();
        // Windows 환경에서 '/C:/...' 형태가 들어오면 'C:/...'로 보정한다.
        if (normalized.matches("^/[A-Za-z]:/.*")) {
            normalized = normalized.substring(1);
        }
        return Path.of(normalized).toAbsolutePath().normalize();
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolver) {
        argumentResolver.add(loginUserArgumentResolver);
    }
}
