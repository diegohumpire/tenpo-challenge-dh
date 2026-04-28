package com.tenpo.dh.challenge.dhapi.config;

import com.tenpo.dh.challenge.dhapi.adapter.in.web.MockPercentageController;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.reactive.config.ApiVersionConfigurer;
import org.springframework.web.reactive.config.PathMatchConfigurer;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebFluxConfig implements WebFluxConfigurer {

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer.usePathSegment(1)
                .addSupportedVersions("1")
                .setDefaultVersion("1");
    }

    @Override
    public void configurePathMatching(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/api",
                HandlerTypePredicate.forAnnotation(RestController.class)
                        .and(HandlerTypePredicate.forAssignableType(MockPercentageController.class).negate())
                        .and(HandlerTypePredicate.forBasePackage("org.springdoc").negate()));
    }

    @Bean
    @ConditionalOnMissingBean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(new Info().title("Tenpo Challenge API").version("1.0.0")
                .description("REST API with dynamic percentage calculation, Redis cache, rate limiting, and audit logging")
                .contact(new Contact().name("Diego Humpire").url("https://github.com/diegohumpire")));
    }
}
