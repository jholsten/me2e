package org.jholsten.me2e.config.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.jholsten.me2e.config.utils.ContainerMapDeserializer
import org.jholsten.me2e.container.Container

/**
 * Model containing the test configuration to use.
 */
class TestConfig(
    /**
     * List of Docker containers to start for the end-to-end test.
     */
    @JsonDeserialize(using = ContainerMapDeserializer::class)
    val containers: Map<String, Container>,

    /**
     * HTTP web servers mocking third party services.
     */
    @JsonProperty("mock-servers")
    val mockServers: MockServerConfig? = null,
) {

}
