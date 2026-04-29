package com.tenpo.dh.challenge.dhapi.bdd.steps;

import com.tenpo.dh.challenge.dhapi.adapter.in.web.filter.RequestHeadersFilter;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

public class RequestHeadersSteps {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ScenarioResponseContext scenarioContext;

    @When("^envío POST /api/v1/calculations con cabeceras obligatorias y num1=([-+]?[\\d.]+) y num2=([-+]?[\\d.]+)$")
    public void envíoPostConCabecerasObligatorias(double num1, double num2) {
        scenarioContext.setLastResponse(webTestClient.post()
                .uri("/api/v1/calculations")
                .header(RequestHeadersFilter.HEADER_TRANSACTIONAL_ID, "txn-bdd-001")
                .header(RequestHeadersFilter.HEADER_USER_ID, "user-bdd-001")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("num1", num1, "num2", num2))
                .exchange());
    }

    @When("^envío POST /api/v1/calculations sin el header \"([^\"]+)\"$")
    public void envíoPostSinElHeader(String missingHeader) {
        WebTestClient.RequestBodySpec spec = webTestClient.post()
                .uri("/api/v1/calculations")
                .contentType(MediaType.APPLICATION_JSON);

        if (!missingHeader.equals(RequestHeadersFilter.HEADER_TRANSACTIONAL_ID)) {
            spec = spec.header(RequestHeadersFilter.HEADER_TRANSACTIONAL_ID, "txn-bdd-001");
        }
        if (!missingHeader.equals(RequestHeadersFilter.HEADER_USER_ID)) {
            spec = spec.header(RequestHeadersFilter.HEADER_USER_ID, "user-bdd-001");
        }

        scenarioContext.setLastResponse(
                spec.bodyValue(Map.of("num1", 5.0, "num2", 5.0)).exchange());
    }

    @When("^envío POST /api/v1/calculations sin cabeceras obligatorias$")
    public void envíoPostSinCabecerasObligatorias() {
        scenarioContext.setLastResponse(webTestClient.post()
                .uri("/api/v1/calculations")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("num1", 5.0, "num2", 5.0))
                .exchange());
    }
}
