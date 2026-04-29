package com.tenpo.dh.challenge.dhapi.bdd.steps;

import com.tenpo.dh.challenge.dhapi.adapter.in.web.filter.RequestHeadersFilter;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class RateLimitingSteps {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ScenarioResponseContext scenarioContext;

    private String clientIp = "127.0.0.1";
    private List<WebTestClient.ResponseSpec> responses = new ArrayList<>();

    @Before
    public void resetRateLimitState() {
        clientIp = "127.0.0.1";
        responses.clear();
    }

    @Given("el IP del cliente es {string}")
    public void elIPDelClienteEs(String ip) {
        this.clientIp = ip;
    }

    @When("^envío (\\d+) solicitudes POST /api/v1/calculations en menos de 60 segundos$")
    public void envíoNSolicitudesPost(int count) {
        responses.clear();
        for (int i = 0; i < count; i++) {
            responses.add(webTestClient.post()
                    .uri("/api/v1/calculations")
                    .header("X-Forwarded-For", clientIp)
                    .header(RequestHeadersFilter.HEADER_TRANSACTIONAL_ID, "txn-rl-" + i)
                    .header(RequestHeadersFilter.HEADER_USER_ID, "user-rl")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("num1", 5.0, "num2", 5.0))
                    .exchange());
        }
    }

    @Given("ya se realizaron {int} solicitudes en el último minuto")
    public void yaSeRealizaronNSolicitudes(int count) {
        for (int i = 0; i < count; i++) {
            webTestClient.post()
                    .uri("/api/v1/calculations")
                    .header("X-Forwarded-For", clientIp)
                    .header(RequestHeadersFilter.HEADER_TRANSACTIONAL_ID, "txn-rl-pre-" + i)
                    .header(RequestHeadersFilter.HEADER_USER_ID, "user-rl")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("num1", 1.0, "num2", 1.0))
                    .exchange()
                    .expectStatus().is2xxSuccessful();
        }
    }

    @When("^envío una cuarta solicitud POST /api/v1/calculations$")
    public void envíoUnaCuartaSolicitud() {
        scenarioContext.setLastResponse(webTestClient.post()
                .uri("/api/v1/calculations")
                .header("X-Forwarded-For", clientIp)
                .header(RequestHeadersFilter.HEADER_TRANSACTIONAL_ID, "txn-rl-4th")
                .header(RequestHeadersFilter.HEADER_USER_ID, "user-rl")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("num1", 5.0, "num2", 5.0))
                .exchange());
    }

    @When("^envío GET /actuator/health$")
    public void envíoGetActuatorHealth() {
        scenarioContext.setLastResponse(webTestClient.get().uri("/actuator/health").exchange());
    }

    @Then("las {int} respuestas tienen status {int}")
    public void lasNRespuestasTienenStatus(int count, int status) {
        assertThat(responses).hasSize(count);
        responses.forEach(r -> r.expectStatus().isEqualTo(status));
    }

    @Then("la respuesta es {int} Too Many Requests")
    public void laRespuestaEs429TooManyRequests(int status) {
        scenarioContext.getLastResponse().expectStatus().isEqualTo(status);
    }

    @And("el header {string} es {string}")
    public void elHeaderEs(String headerName, String expectedValue) {
        scenarioContext.getLastResponse().expectHeader().valueEquals(headerName, expectedValue);
    }

    @And("el header {string} está presente")
    public void elHeaderEstáPresente(String headerName) {
        scenarioContext.getLastResponse().expectHeader().exists(headerName);
    }

    @Then("la respuesta es {int} OK sin aplicar rate limiting")
    public void laRespuestaEs200OKSinRateLimiting(int status) {
        scenarioContext.getLastResponse().expectStatus().isEqualTo(status);
    }
}
