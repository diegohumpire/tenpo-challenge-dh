package com.tenpo.dh.challenge.dhapi.bdd.steps;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Shared response holder for Cucumber scenarios.
 * A fresh instance is created for each scenario (cucumber-glue scope),
 * so all step definition classes access the same {@link WebTestClient.ResponseSpec}
 * within a single scenario without cross-scenario contamination.
 */
@Component
@Scope("cucumber-glue")
public class ScenarioResponseContext {

    private WebTestClient.ResponseSpec lastResponse;

    public WebTestClient.ResponseSpec getLastResponse() {
        return lastResponse;
    }

    public void setLastResponse(WebTestClient.ResponseSpec lastResponse) {
        this.lastResponse = lastResponse;
    }
}
