package dev.chol.globechat.config;

import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Web configuration for serving the Angular SPA.
 * <p>
 * This configuration ensures that:
 * - Static resources are served from /static/ directory
 * - All non-API routes are forwarded to index.html for Angular client-side routing
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(@NonNull String resourcePath, @NonNull Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);

                        // If the resource exists and is readable, serve it
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }

                        // For API requests, return null to let Spring handle 404
                        if (resourcePath.startsWith("api/")) {
                            return null;
                        }

                        // For all other requests (SPA routes), return index.html
                        return new ClassPathResource("/static/index.html");
                    }
                });
    }
}
