package org.jholsten.me2e.config.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Settings for running the End-to-End tests, parsed from the `settings` section of the config file.
 */
data class TestSettings(
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
)
