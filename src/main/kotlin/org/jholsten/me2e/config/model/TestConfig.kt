package org.jholsten.me2e.config.model

/**
 * Model containing the test configuration to use.
 */
class TestConfig(
    /**
     * Configuration for all HTTP requests to [org.jholsten.me2e.container.microservice.MicroserviceContainer] instances.
     */
    val requests: RequestConfig = RequestConfig(10, 10, 10),

    /**
     * Configuration of the test environment.
     */
    val environment: TestEnvironmentConfig,
)
