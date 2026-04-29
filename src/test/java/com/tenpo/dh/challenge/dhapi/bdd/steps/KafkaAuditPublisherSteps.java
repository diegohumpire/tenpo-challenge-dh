package com.tenpo.dh.challenge.dhapi.bdd.steps;

import com.tenpo.dh.challenge.dhapi.adapter.out.persistence.AuditLogR2dbcDao;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Step definitions specific to the Kafka audit publisher BDD scenarios.
 *
 * Reuses most steps from existing step classes (e.g., {@link CalculationSteps},
 * {@link AuditLogSteps}). This class only adds steps unique to the Kafka flow.
 */
public class KafkaAuditPublisherSteps {

    @Autowired
    private AuditLogR2dbcDao auditLogR2dbcDao;

    @Before("@kafka")
    public void clearAuditLogsBeforeKafkaScenario() {
        auditLogR2dbcDao.deleteAll().block();
    }

    @And("espero a que el registro Kafka se complete")
    public void esperoAQueElRegistroKafkaSeComplete() throws InterruptedException {
        // Kafka introduce latencia adicional: produce → broker → consume → DB persist
        Thread.sleep(3000);
    }
}
