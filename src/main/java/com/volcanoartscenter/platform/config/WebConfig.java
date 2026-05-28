package com.volcanoartscenter.platform.config;

import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${platform.storage.local-upload-dir:${user.home}/.volcano-platform/uploads}")
    private String localUploadDir;

    @Bean
    public LayoutDialect layoutDialect() {
        return new LayoutDialect();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String managedUploadLocation = Path.of(localUploadDir).toAbsolutePath().normalize().toUri().toString() + "/";
        String projectUploadLocation = Path.of("uploads").toAbsolutePath().normalize().toUri().toString() + "/";
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(
                        managedUploadLocation,
                        projectUploadLocation,
                        "classpath:/static/uploads/"
                )
                .setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic());
    }
}
