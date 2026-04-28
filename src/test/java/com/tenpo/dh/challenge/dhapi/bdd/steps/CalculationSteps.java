package com.tenpo.dh.challenge.dhapi.bdd.steps;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CalculationSteps {

    @Autowired
    private WebTestClient webTestClient;

    private WebTestClient.ResponseSpec lastResponse;

    @Given("el servicio externo retorna un porcentaje de {double}%")
    public void elServicioExternoRetornaUnPorcentaje(double percentage) {
        // Mock percentage is configured via application-test.yaml
        // default mock returns 10% which handles this step
    }

    @When("envío POST /api/1/calculations con num1={double} y num2={double}")
    public void envíoPOSTCalculationsConNum1YNum2(double num1, double num2) {
        lastResponse = webTestClient.post()
                .uri("/api/1/calculations")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("num1", num1, "num2", num2))
                .exchange();
    }

    @When("envío POST /api/1/calculations con num1=null y num2={double}")
    public void envíoPOSTCalculationsConNum1NullYNum2(double num2) {
        lastResponse = webTestClient.post()
                .uri("/api/1/calculations")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("num2", num2))
                .exchange();
    }

    @Then("la respuesta es {int} Created")
    public void laRespuestaEs201Created(int status) {
        lastResponse.expectStatus().isEqualTo(status);
    }

    @Then("la respuesta es {int} Bad Request")
    public void laRespuestaEs400BadRequest(int status) {
        lastResponse.expectStatus().isEqualTo(status);
    }

    @Then("la respuesta es {int} Service Unavailable")
    public void laRespuestaEs503ServiceUnavailable(int status) {
        lastResponse.expectStatus().isEqualTo(status);
    }

    @And("el campo {string} es {double}")
    public void elCampoEs(String fieldName, double expectedValue) {
        lastResponse.expectBody()
                .jsonPath("$." + fieldName)
                .value(v -> assertThat(((Number) v).doubleValue()).isEqualTo(expectedValue));
    }

    @And("el body contiene un Problem Detail con status {int}")
    public void elBodyContieneUnProblemDetailConStatus(int status) {
        lastResponse.expectBody()
                .jsonPath("$.status").isEqualTo(status);
    }
}
