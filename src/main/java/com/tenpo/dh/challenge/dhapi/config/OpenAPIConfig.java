package com.tenpo.dh.challenge.dhapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Tenpo Challenge API")
                        .version("1.0.0")
                        .description(
                                "REST API with dynamic percentage calculation, Redis cache, rate limiting, and audit logging")
                        .contact(new Contact()
                                .name("Diego Humpire")
                                .url("https://github.com/diegohumpire")));
    }
}
