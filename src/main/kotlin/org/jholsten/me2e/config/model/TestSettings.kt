package org.jholsten.me2e.config.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.jholsten.me2e.Me2eExtension

/**
 * Settings for running the End-to-End tests, parsed from the `settings` section of the config file.
 */
data class TestSettings internal constructor(
    /**
     * Configuration for Docker/Docker-Compose.
     */
    val docker: DockerConfig = DockerConfig(),

    /**
     * Configuration for all HTTP requests to [org.jholsten.me2e.container.microservice.MicroserviceContainer] instances.
     */
    val requests: RequestConfig = RequestConfig(),

    /**
     * Configuration for all [org.jholsten.me2e.mock.MockServer] instances.
     */
    @JsonProperty("mock-servers")
    val mockServers: MockServerConfig = MockServerConfig(),

    /**
     * Configuration for resetting the state of containers, Mock Servers and databases after each test.
     * @see Me2eExtension.afterEach
     */
    @JsonProperty("state-reset")
    val stateReset: StateResetConfig = StateResetConfig(),

    /**
     * Whether to assert that all containers are healthy before each test.
     * If activated, it is checked whether all containers for which a healthcheck is defined are currently healthy
     * before each test. Restarts containers if necessary and aborts tests if containers remain unhealthy.
     * @see Me2eExtension.beforeEach
     */
    @JsonProperty("assert-healthy")
    val assertHealthy: Boolean = true,
)
