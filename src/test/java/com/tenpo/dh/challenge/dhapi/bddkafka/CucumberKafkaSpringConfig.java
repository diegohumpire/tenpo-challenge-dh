package com.tenpo.dh.challenge.dhapi.bddkafka;

import com.tenpo.dh.challenge.dhapi.AbstractKafkaIntegrationTest;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Spring context configuration for the Kafka BDD test suite.
 *
 * Resides in a sibling package ({@code bddkafka}) intentionally to avoid glue scanning
 * conflicts with the standard {@code CucumberSpringConfig} that lives in the {@code bdd}
 * package. The Kafka runner's glue explicitly points here.
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class CucumberKafkaSpringConfig extends AbstractKafkaIntegrationTest {
}
