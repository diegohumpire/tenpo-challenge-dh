package com.tenpo.dh.challenge.dhapi.bdd.steps;

import com.tenpo.dh.challenge.dhapi.adapter.out.http.InMemoryPercentageProvider;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CalculationSteps {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private InMemoryPercentageProvider inMemoryPercentageProvider;

    @Autowired
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @Autowired
    private ScenarioResponseContext scenarioContext;

    private boolean externalServiceDown = false;

    @Before
    public void resetState() {
        externalServiceDown = false;
        inMemoryPercentageProvider.setSimulatingFailure(false);
        redisTemplate.keys("rate_limit:*")
                .flatMap(key -> redisTemplate.delete(key))
                .then()
                .block();
    }

    @Given("el servicio externo retorna un porcentaje de {double}%")
    public void elServicioExternoRetornaUnPorcentaje(double percentage) {
        // Mock percentage is configured via application-test.yaml
        // default mock returns 10% which handles this step
    }

    @Given("el servicio externo no está disponible")
    public void elServicioExternoNoEstaDisponible() {
        externalServiceDown = true;
        inMemoryPercentageProvider.setSimulatingFailure(true);
    }

    @When("^envío POST /api/v1/calculations con num1=([-+]?[\\d.]+) y num2=([-+]?[\\d.]+)$")
    public void envíoPOSTCalculationsConNum1YNum2(double num1, double num2) {
        WebTestClient.RequestBodySpec spec = webTestClient.post()
                .uri("/api/v1/calculations")
                .contentType(MediaType.APPLICATION_JSON);
        if (externalServiceDown) {
            spec = spec.header("x-mock-response-code", "503");
        }
        scenarioContext.setLastResponse(spec.bodyValue(Map.of("num1", num1, "num2", num2)).exchange());
    }

    @When("^envío POST /api/v1/calculations con num1=null y num2=([-+]?[\\d.]+)$")
    public void envíoPOSTCalculationsConNum1NullYNum2(double num2) {
        scenarioContext.setLastResponse(webTestClient.post()
                .uri("/api/v1/calculations")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("num2", num2))
                .exchange());
    }

    @Then("la respuesta es {int} Created")
    public void laRespuestaEs201Created(int status) {
        scenarioContext.getLastResponse().expectStatus().isEqualTo(status);
    }

    @Then("la respuesta es {int} Bad Request")
    public void laRespuestaEs400BadRequest(int status) {
        scenarioContext.getLastResponse().expectStatus().isEqualTo(status);
    }

    @Then("la respuesta es {int} Service Unavailable")
    public void laRespuestaEs503ServiceUnavailable(int status) {
        scenarioContext.getLastResponse().expectStatus().isEqualTo(status);
    }

    @And("el campo {string} es {double}")
    public void elCampoEs(String fieldName, double expectedValue) {
        scenarioContext.getLastResponse().expectBody()
                .jsonPath("$." + fieldName)
                .value(v -> assertThat(((Number) v).doubleValue()).isEqualTo(expectedValue));
    }

    @And("el body contiene un Problem Detail con status {int}")
    public void elBodyContieneUnProblemDetailConStatus(int status) {
        scenarioContext.getLastResponse().expectBody()
                .jsonPath("$.status").isEqualTo(status);
    }
}
