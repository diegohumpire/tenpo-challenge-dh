package com.tenpo.dh.challenge.dhapi.config;

import com.tenpo.dh.challenge.dhapi.adapter.in.web.annotation.NonVersionApi;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.reactive.config.ApiVersionConfigurer;
import org.springframework.web.reactive.config.PathMatchConfigurer;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
public class WebFluxConfig implements WebFluxConfigurer {

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer
                .usePathSegment(1)
                .addSupportedVersions("1", "2")
                .setDefaultVersion("1")
                .setVersionRequired(false);
    }

    /**
     * Adds /api/v1 prefix to all versioned @RestController beans.
     * MockPercentageController (/mock/**) and springdoc (/v3/**, /swagger-ui**)
     * are explicitly excluded so they remain unversioned.
     */
    @Override
    public void configurePathMatching(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(
                "/api/v{version}",
                HandlerTypePredicate.forAnnotation(RestController.class)
                        .and(HandlerTypePredicate.forBasePackage("org.springdoc").negate())
                        .and(HandlerTypePredicate.forAnnotation(NonVersionApi.class).negate()) // Exclude exceptional
        );
    }
}
