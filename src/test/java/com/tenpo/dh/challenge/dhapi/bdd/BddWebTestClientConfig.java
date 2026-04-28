package com.tenpo.dh.challenge.dhapi.bdd;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.reactive.server.WebTestClient;

@Configuration
public class BddWebTestClientConfig {

    @Bean
    public WebTestClient webTestClient(@Value("${local.server.port:8080}") int port) {
        return WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }
}
