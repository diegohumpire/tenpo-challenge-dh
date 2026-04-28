package com.tenpo.dh.challenge.dhapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.ApiVersionConfigurer;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebFluxConfig implements WebFluxConfigurer {

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer
                .usePathSegment(1)
                .addSupportedVersions("1")
                .setDefaultVersion("1");
    }

    @Bean
    @ConditionalOnMissingBean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Tenpo Challenge API")
                        .version("1.0.0")
                        .description("REST API with dynamic percentage calculation, Redis cache, rate limiting, and audit logging")
                        .contact(new Contact().name("Diego Humpire").url("https://github.com/diegohumpire")));
    }
}

