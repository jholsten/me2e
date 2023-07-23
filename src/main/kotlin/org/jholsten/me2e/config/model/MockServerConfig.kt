package org.jholsten.me2e.config.model

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.jholsten.me2e.config.utils.MockServerMapDeserializer
import org.jholsten.me2e.mock.MockServer

/**
 * Configuration for HTTP web servers mocking third party services.
 */
class MockServerConfig(
    /**
     * Port where mocked web servers are exposed
     */
    val port: Int,
    /**
     * Map of service name and stubs for the third party services
     */
    @JsonDeserialize(using = MockServerMapDeserializer::class)
    val services: Map<String, MockServer> = mapOf(),
)
