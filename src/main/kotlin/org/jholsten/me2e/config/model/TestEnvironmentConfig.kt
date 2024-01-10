package org.jholsten.me2e.config.model

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.jholsten.me2e.config.utils.TestEnvironmentConfigDeserializer
import org.jholsten.me2e.container.Container
import org.jholsten.me2e.mock.MockServer

/**
 * Model for the configuration of the test environment to use for the End-to-End-Tests.
 */
@JsonDeserialize(using = TestEnvironmentConfigDeserializer::class)
class TestEnvironmentConfig(
    /**
     * Path to Docker-Compose file. Needs to be located in `resources` folder.
     */
    val dockerCompose: String,

    /**
     * Self-managed containers from Docker-Compose file.
     */
    val containers: Map<String, Container>,

    /**
     * HTTP web servers mocking third party services.
     */
    val mockServers: Map<String, MockServer> = mapOf(),
)
