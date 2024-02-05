package org.jholsten.me2e.config.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.jholsten.me2e.config.utils.TestConfigDeserializer

/**
 * Model containing the test configuration to use.
 */
@JsonDeserialize(using = TestConfigDeserializer::class)
class TestConfig(
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
     * Configuration of the test environment.
     */
    val environment: TestEnvironmentConfig,
)
