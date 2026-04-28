package com.tenpo.dh.challenge.dhapi.bdd.steps;

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

    private String clientIp = "127.0.0.1";
    private List<WebTestClient.ResponseSpec> responses = new ArrayList<>();
    private WebTestClient.ResponseSpec lastResponse;

    @Given("el IP del cliente es {string}")
    public void elIPDelClienteEs(String ip) {
        this.clientIp = ip;
    }

    @When("envío {int} solicitudes POST /api/v1/calculations en menos de 60 segundos")
    public void envíoNSolicitudesPost(int count) {
        responses.clear();
        for (int i = 0; i < count; i++) {
            responses.add(webTestClient.post()
                    .uri("/api/v1/calculations")
                    .header("X-Forwarded-For", clientIp)
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
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("num1", 1.0, "num2", 1.0))
                    .exchange()
                    .returnResult(String.class);
        }
    }

    @When("envío una cuarta solicitud POST /api/v1/calculations")
    public void envíoUnaCuartaSolicitud() {
        lastResponse = webTestClient.post()
                .uri("/api/v1/calculations")
                .header("X-Forwarded-For", clientIp)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("num1", 5.0, "num2", 5.0))
                .exchange();
    }

    @When("envío GET /actuator/health")
    public void envíoGetActuatorHealth() {
        lastResponse = webTestClient.get().uri("/actuator/health").exchange();
    }

    @Then("las {int} respuestas tienen status {int}")
    public void lasNRespuestasTienenStatus(int count, int status) {
        assertThat(responses).hasSize(count);
        responses.forEach(r -> r.expectStatus().isEqualTo(status));
    }

    @Then("la respuesta es {int} Too Many Requests")
    public void laRespuestaEs429TooManyRequests(int status) {
        lastResponse.expectStatus().isEqualTo(status);
    }

    @And("el header {string} es {string}")
    public void elHeaderEs(String headerName, String expectedValue) {
        lastResponse.expectHeader().valueEquals(headerName, expectedValue);
    }

    @And("el header {string} está presente")
    public void elHeaderEstáPresente(String headerName) {
        lastResponse.expectHeader().exists(headerName);
    }

    @Then("la respuesta es {int} OK sin aplicar rate limiting")
    public void laRespuestaEs200OKSinRateLimiting(int status) {
        lastResponse.expectStatus().isEqualTo(status);
    }
}
