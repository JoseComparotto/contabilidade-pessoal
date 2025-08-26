package me.josecomparotto.contabilidade_pessoal.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    public WebConfig() {
    }

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins("*")
            .allowedMethods("*");
    }

    @Override
    public void addViewControllers(@NonNull ViewControllerRegistry registry) {
        String docsPath = "/api/docs";
        registry.addRedirectViewController("/docs", docsPath);
        registry.addRedirectViewController("/", docsPath);
        registry.addRedirectViewController("", docsPath);
    }
}
