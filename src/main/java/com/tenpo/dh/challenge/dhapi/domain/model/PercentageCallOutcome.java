package com.tenpo.dh.challenge.dhapi.domain.model;

import java.math.BigDecimal;

/**
 * Result of a percentage resolution, carrying the value alongside optional HTTP metadata.
 * HTTP-based providers populate all fields; non-HTTP providers (e.g. in-memory stub)
 * only populate {@link #percentage}.
 */
public record PercentageCallOutcome(
        BigDecimal percentage,
        String endpoint,
        String requestHeaders,
        String responseHeaders
) {
    /** Creates an outcome without HTTP metadata (e.g. from an in-memory stub). */
    public static PercentageCallOutcome of(BigDecimal percentage) {
        return new PercentageCallOutcome(percentage, null, null, null);
    }

    /** Creates an outcome with full HTTP metadata captured from a real HTTP call. */
    public static PercentageCallOutcome ofHttp(BigDecimal percentage,
                                                String endpoint,
                                                String requestHeaders,
                                                String responseHeaders) {
        return new PercentageCallOutcome(percentage, endpoint, requestHeaders, responseHeaders);
    }
}
