package org.jholsten.me2e.config.model

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.jholsten.me2e.config.utils.ContainerMapDeserializer
import org.jholsten.me2e.container.Container
import org.jholsten.me2e.mock.MockServer

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
     * List of third party services to be mocked.
     */
    val mockServers: List<MockServer> = listOf(),
) {

}
