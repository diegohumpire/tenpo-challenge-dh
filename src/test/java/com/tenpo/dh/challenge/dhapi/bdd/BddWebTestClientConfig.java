package com.tenpo.dh.challenge.dhapi.bdd;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.reactive.server.WebTestClient;

@Configuration
public class BddWebTestClientConfig {

    @Bean
    public WebTestClient webTestClient(ApplicationContext applicationContext) {
        return WebTestClient.bindToApplicationContext(applicationContext)
                .configureClient()
                .build();
    }
}
