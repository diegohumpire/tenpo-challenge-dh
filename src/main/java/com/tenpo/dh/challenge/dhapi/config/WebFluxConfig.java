package com.tenpo.dh.challenge.dhapi.config;

import com.tenpo.dh.challenge.dhapi.annotation.NonVersionApi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import jakarta.annotation.Nullable;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.accept.ApiVersionParser;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.reactive.accept.ApiVersionResolver;
import org.springframework.web.reactive.config.ApiVersionConfigurer;
import org.springframework.web.reactive.config.PathMatchConfigurer;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;

@Configuration
public class WebFluxConfig implements WebFluxConfigurer {

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer
                .addSupportedVersions("1", "2")
                .setDefaultVersion("1")
                .setVersionRequired(false)
                // Use a flexible resolver that parses the version only after verifying the path
                // structure
                .useVersionResolver(new ApiPathVersionResolver())
                .setVersionParser(new SimpleVersionParser());
    }

    /**
     * Adds /api/v1 prefix to all versioned @RestController beans.
     * MockPercentageController (/mock/**) and springdoc (/v3/**, /swagger-ui**)
     * are explicitly excluded so they remain unversioned.
     */
    @Override
    public void configurePathMatching(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(
                "/api/v{version:(?:1|2)}",
                HandlerTypePredicate.forAnnotation(RestController.class)
                        .and(HandlerTypePredicate.forBasePackage("org.springdoc").negate())
                        .and(HandlerTypePredicate.forAnnotation(NonVersionApi.class).negate()) // Exclude exceptional
        );
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
                        .description(
                                "REST API with dynamic percentage calculation, Redis cache, rate limiting, and audit logging")
                        .contact(new Contact()
                                .name("Diego Humpire")
                                .url("https://github.com/diegohumpire")));
    }

    /**
     * * A flexible resolver that verifies the path structure and parses the version
     */
    private static class ApiPathVersionResolver implements ApiVersionResolver {

        @Override
        public @Nullable String resolveVersion(ServerWebExchange exchange) {
            String uri = exchange.getRequest().getURI().getPath();
            if (uri == null || !uri.startsWith("/api/v")) {
                return null; // Ignore APIs that do not follow version rules
            }

            int start = 6; // Start point after "/api/v"
            int end = uri.indexOf('/', start);

            if (end != -1) {
                return uri.substring(start, end);
            }
            return null;
        }
    }

    /**
     * * Parser that converts version strings into the standard format used
     * internally.
     */
    private static class SimpleVersionParser implements ApiVersionParser<String> {

        @Override
        public String parseVersion(String version) {
            if (!StringUtils.hasText(version)) {
                return null;
            }
            if (version.startsWith("v") || version.startsWith("V")) {
                version = version.substring(1);
            }
            int dotIndex = version.indexOf('.');
            return dotIndex == -1 ? version : version.substring(0, dotIndex);
        }
    }
}
