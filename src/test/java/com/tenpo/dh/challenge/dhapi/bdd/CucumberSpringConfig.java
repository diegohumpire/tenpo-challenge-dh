package com.tenpo.dh.challenge.dhapi.bdd;

import com.tenpo.dh.challenge.dhapi.AbstractIntegrationTest;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = "mock.percentage.base-url=http://localhost:${local.server.port}")
public class CucumberSpringConfig extends AbstractIntegrationTest {}

