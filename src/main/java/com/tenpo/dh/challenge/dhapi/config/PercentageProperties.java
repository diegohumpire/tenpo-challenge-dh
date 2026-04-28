package com.tenpo.dh.challenge.dhapi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@Data
@ConfigurationProperties(prefix = "percentage")
public class PercentageProperties {

    private String provider = "memory";
    private InMemory inMemory = new InMemory();
    private PostmanMock postmanMock = new PostmanMock();
    private External external = new External();

    @Data
    public static class InMemory {
        /** Default percentage value returned when no x-mock-percentage header is present. */
        private BigDecimal value = BigDecimal.TEN;
    }

    @Data
    public static class PostmanMock {
        private String baseUrl = "https://ec995055-c0c3-4482-aa85-89f5660540f0.mock.pstmn.io";
        private String path = "/mock/percentage";
    }

    @Data
    public static class External {
        private String baseUrl = "http://localhost:8080";
        private String path = "/percentage";
    }
}
