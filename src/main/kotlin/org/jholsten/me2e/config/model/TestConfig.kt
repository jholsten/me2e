package org.jholsten.me2e.config.model

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.jholsten.me2e.config.utils.TestConfigDeserializer

/**
 * Model containing the test configuration to use.
 */
@JsonDeserialize(using = TestConfigDeserializer::class)
class TestConfig(
    /**
     * Configuration for all HTTP requests to [org.jholsten.me2e.container.microservice.MicroserviceContainer] instances.
     */
    val requests: RequestConfig = RequestConfig(),

    /**
     * Configuration of the test environment.
     */
    val environment: TestEnvironmentConfig,
)
