package com.tenpo.dh.challenge.dhapi.bddkafka;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/**
 * Cucumber runner for the Kafka audit publisher BDD scenarios (tagged {@code @kafka}).
 *
 * Uses a separate glue that includes this package (for {@link CucumberKafkaSpringConfig})
 * plus the shared {@code bdd.steps} package — but NOT the {@code bdd} package, avoiding
 * the duplicate {@code @CucumberContextConfiguration} conflict with the standard suite.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME,
        value = "com.tenpo.dh.challenge.dhapi.bddkafka,com.tenpo.dh.challenge.dhapi.bdd.steps")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "@kafka")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME,
        value = "pretty, html:target/cucumber-kafka-report.html")
@Testcontainers(disabledWithoutDocker = true)
public class KafkaCucumberRunnerTest {
}
