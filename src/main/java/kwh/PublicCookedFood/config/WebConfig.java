package kwh.PublicCookedFood.config;

import kwh.PublicCookedFood.config.oauth2.LoginAccountArgumentResolver;
import kwh.PublicCookedFood.config.properties.StorageProperties;
import kwh.PublicCookedFood.storage.StorageCategory;
import kwh.PublicCookedFood.storage.StoragePathUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RequiredArgsConstructor
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final LoginAccountArgumentResolver loginAccountArgumentResolver;
    private final StorageProperties storageProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = StoragePathUtils.resolveUploadPath(storageProperties.dir());
        Path imagePath = uploadPath.resolve(StorageCategory.IMAGE.getDirectoryName()).normalize();
        Path filePath = uploadPath.resolve(StorageCategory.ATTACHMENT.getDirectoryName()).normalize();

        registry.addResourceHandler("/images/**")
                .addResourceLocations(toResourceLocation(imagePath))
                .resourceChain(true)
                .addResolver(imageResourceResolver(imagePath, uploadPath));

        registry.addResourceHandler("/files/**")
                .addResourceLocations(toResourceLocation(filePath));
    }

    private PathResourceResolver imageResourceResolver(Path imagePath, Path uploadPath) {
        return new PathResourceResolver() {
            @Override
            protected Resource getResource(String resourcePath, Resource location) throws IOException {
                Resource imageResource = resolveFileResource(imagePath, resourcePath);
                if (imageResource != null) {
                    return imageResource;
                }
                return resolveFileResource(uploadPath, resourcePath);
            }
        };
    }

    private Resource resolveFileResource(Path basePath, String resourcePath) throws IOException {
        Path resolvedPath = StoragePathUtils.resolvePathUnderDirectory(basePath, resourcePath);
        if (resolvedPath == null || !Files.isRegularFile(resolvedPath)) {
            return null;
        }

        UrlResource resource = new UrlResource(resolvedPath.toUri());
        return resource.exists() && resource.isReadable() ? resource : null;
    }

    private String toResourceLocation(Path path) {
        String resourceLocation = path.toUri().toString();
        return resourceLocation.endsWith("/") ? resourceLocation : resourceLocation + "/";
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolver) {
        argumentResolver.add(loginAccountArgumentResolver);
    }
}
