package com.tenpo.dh.challenge.dhapi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {

    /** Maximum number of requests allowed per time window per IP. */
    private int maxRequests = 3;

    /** Size of the sliding window in seconds. */
    private long windowSeconds = 60;
}
