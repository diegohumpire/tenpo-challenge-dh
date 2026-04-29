package com.tenpo.dh.challenge.dhapi.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Activates {@link PercentageProperties} binding and imports
 * {@link PercentageProviderRegistrar} to dynamically register the correct
 * {@link com.tenpo.dh.challenge.dhapi.domain.port.out.PercentageProvider} implementation.
 */
@Configuration
@Import({PercentageProviderRegistrar.class, ResilienceConfig.class})
@EnableConfigurationProperties(PercentageProperties.class)
public class PercentageProviderConfig {
}
