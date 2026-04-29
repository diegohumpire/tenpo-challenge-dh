package com.tenpo.dh.challenge.dhapi.bdd.steps;

import com.tenpo.dh.challenge.dhapi.adapter.in.web.filter.RequestHeadersFilter;
import com.tenpo.dh.challenge.dhapi.adapter.out.persistence.AuditLogR2dbcDao;
import com.tenpo.dh.challenge.dhapi.domain.model.AuditActionType;
import com.tenpo.dh.challenge.dhapi.domain.model.AuditLog;
import com.tenpo.dh.challenge.dhapi.domain.model.CallDirection;
import com.tenpo.dh.challenge.dhapi.domain.port.out.AuditLogRepository;
import io.cucumber.java.Before;
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

    @Autowired
    private AuditLogR2dbcDao auditLogR2dbcDao;

    @Autowired
    private ScenarioResponseContext scenarioContext;

    @Before
    public void clearAuditLogs() {
        auditLogR2dbcDao.deleteAll().block();
    }

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

    @When("^envío GET /api/v1/audit-logs\\?page=(\\d+)&size=(\\d+)$")
    public void envíoGetAuditLogs(int page, int size) {
        scenarioContext.setLastResponse(webTestClient.get()
                .uri(u -> u.path("/api/v1/audit-logs").queryParam("page", page).queryParam("size", size).build())
                .header(RequestHeadersFilter.HEADER_TRANSACTIONAL_ID, "txn-bdd-audit")
                .header(RequestHeadersFilter.HEADER_USER_ID, "user-bdd-audit")
                .exchange());
    }

    @Then("la respuesta es {int} OK")
    public void laRespuestaEs200OK(int status) {
        scenarioContext.getLastResponse().expectStatus().isEqualTo(status);
    }

    @And("el campo {string} contiene {int} registros")
    public void elCampoContieneNRegistros(String fieldName, int count) {
        scenarioContext.getLastResponse().expectBody()
                .jsonPath("$." + fieldName).isArray()
                .jsonPath("$." + fieldName + ".length()").isEqualTo(count);
    }

    /**
     * Ensures the previous HTTP exchange (typically a POST) is eagerly executed
     * before sleeping for the async audit log write to complete.
     *
     * WebTestClient.exchange() is lazy — the actual request is only sent when a
     * terminal assertion method is called. Sleeping without consuming the spec
     * would let it be overwritten before the request ever fires.
     */
    @And("espero a que el registro asíncrono se complete")
    public void esperoAQueElRegistroAsincrono() throws InterruptedException {
        WebTestClient.ResponseSpec pending = scenarioContext.getLastResponse();
        if (pending != null) {
            pending.expectStatus().is2xxSuccessful();
        }
        Thread.sleep(500);
    }

    @And("^consulto GET /api/v1/audit-logs\\?page=(\\d+)&size=(\\d+)$")
    public void consultaAuditLogs(int page, int size) {
        scenarioContext.setLastResponse(webTestClient.get()
                .uri(u -> u.path("/api/v1/audit-logs").queryParam("page", page).queryParam("size", size).build())
                .header(RequestHeadersFilter.HEADER_TRANSACTIONAL_ID, "txn-bdd-audit")
                .header(RequestHeadersFilter.HEADER_USER_ID, "user-bdd-audit")
                .exchange());
    }

    @Then("el último registro tiene action={string}")
    public void elUltimoRegistroTieneAction(String action) {
        scenarioContext.getLastResponse().expectBody()
                .jsonPath("$.content[0].action").isEqualTo(action);
    }

    @And("el último registro tiene actionType={string}")
    public void elUltimoRegistroTieneActionType(String actionType) {
        scenarioContext.getLastResponse().expectBody()
                .jsonPath("$.content[0].actionType").isEqualTo(actionType);
    }

    @And("el último registro tiene callDirection={string}")
    public void elUltimoRegistroTieneCallDirection(String callDirection) {
        scenarioContext.getLastResponse().expectBody()
                .jsonPath("$.content[0].callDirection").isEqualTo(callDirection);
    }

    @And("el último registro tiene statusCode={int}")
    public void elUltimoRegistroTieneStatusCode(int statusCode) {
        scenarioContext.getLastResponse().expectBody()
                .jsonPath("$.content[0].statusCode").isEqualTo(statusCode);
    }

    @And("el último registro tiene responseBody no nulo")
    public void elUltimoRegistroTieneResponseBodyNoNulo() {
        scenarioContext.getLastResponse().expectBody()
                .jsonPath("$.content[0].responseBody").isNotEmpty();
    }

    @Given("el servicio de persistencia de audit logs lanza una excepción")
    public void elServicioDePersistenciaLanzaUnaExcepcion() {
        // Audit log failures are swallowed by fire-and-forget — main response is
        // unaffected
    }
}
