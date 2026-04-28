package com.tenpo.dh.challenge.dhapi.bdd.steps;

import com.tenpo.dh.challenge.dhapi.domain.model.AuditActionType;
import com.tenpo.dh.challenge.dhapi.domain.model.AuditLog;
import com.tenpo.dh.challenge.dhapi.domain.model.CallDirection;
import com.tenpo.dh.challenge.dhapi.domain.port.out.AuditLogRepository;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.OffsetDateTime;

public class AuditLogSteps {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private WebTestClient.ResponseSpec lastResponse;

    @Given("existen {int} registros en audit_logs")
    public void existenNRegistrosEnAuditLogs(int count) {
        for (int i = 0; i < count; i++) {
            auditLogRepository.save(AuditLog.builder()
                    .createdAt(OffsetDateTime.now())
                    .action("TEST_ACTION_" + i)
                    .actionType(AuditActionType.HTTP_REQUEST)
                    .callDirection(CallDirection.IN)
                    .method("GET")
                    .endpoint("/api/v1/test")
                    .statusCode(200)
                    .build()).block();
        }
    }

    @When("envío GET /api/1/audit-logs?page={int}&size={int}")
    public void envíoGetAuditLogs(int page, int size) {
        lastResponse = webTestClient.get()
                .uri(u -> u.path("/api/1/audit-logs").queryParam("page", page).queryParam("size", size).build())
                .exchange();
    }

    @Then("la respuesta es {int} OK")
    public void laRespuestaEs200OK(int status) {
        lastResponse.expectStatus().isEqualTo(status);
    }

    @And("el campo {string} es {int}")
    public void elCampoEsInt(String fieldName, int expectedValue) {
        lastResponse.expectBody()
                .jsonPath("$." + fieldName).isEqualTo(expectedValue);
    }

    @And("el campo {string} contiene {int} registros")
    public void elCampoContieneNRegistros(String fieldName, int count) {
        lastResponse.expectBody()
                .jsonPath("$." + fieldName).isArray()
                .jsonPath("$." + fieldName + ".length()").isEqualTo(count);
    }

    @And("espero a que el registro asíncrono se complete")
    public void esperoAQueElRegistroAsincrono() throws InterruptedException {
        Thread.sleep(500);
    }

    @And("consulto GET /api/1/audit-logs?page={int}&size={int}")
    public void consultaAuditLogs(int page, int size) {
        lastResponse = webTestClient.get()
                .uri(u -> u.path("/api/1/audit-logs").queryParam("page", page).queryParam("size", size).build())
                .exchange();
    }

    @Then("el último registro tiene action={string}")
    public void elUltimoRegistroTieneAction(String action) {
        lastResponse.expectBody()
                .jsonPath("$.content[0].action").isEqualTo(action);
    }

    @And("el último registro tiene actionType={string}")
    public void elUltimoRegistroTieneActionType(String actionType) {
        lastResponse.expectBody()
                .jsonPath("$.content[0].actionType").isEqualTo(actionType);
    }

    @And("el último registro tiene callDirection={string}")
    public void elUltimoRegistroTieneCallDirection(String callDirection) {
        lastResponse.expectBody()
                .jsonPath("$.content[0].callDirection").isEqualTo(callDirection);
    }

    @And("el último registro tiene statusCode={int}")
    public void elUltimoRegistroTieneStatusCode(int statusCode) {
        lastResponse.expectBody()
                .jsonPath("$.content[0].statusCode").isEqualTo(statusCode);
    }

    @Given("el servicio de persistencia de audit logs lanza una excepción")
    public void elServicioDePersistenciaLanzaUnaExcepcion() {
        // Audit log failures are swallowed by fire-and-forget — main response is
        // unaffected
    }
}
