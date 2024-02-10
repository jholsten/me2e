package org.jholsten.me2e.config.parser.deserializer

import org.jholsten.me2e.config.model.RequestConfig
import org.jholsten.me2e.config.model.TestConfig
import org.jholsten.me2e.config.model.TestEnvironmentConfig
import org.jholsten.me2e.config.model.TestSettings
import org.jholsten.me2e.config.parser.ConfigValidator
import org.jholsten.me2e.container.microservice.MicroserviceContainer
import org.jholsten.me2e.mock.MockServer
import org.jholsten.me2e.mock.stubbing.MockServerStub
import org.jholsten.me2e.mock.stubbing.request.MockServerStubRequestMatcher
import org.jholsten.me2e.mock.stubbing.response.MockServerStubResponse
import org.jholsten.me2e.parsing.exception.ValidationException
import org.jholsten.util.assertDoesNotThrow
import kotlin.test.*

internal class ConfigValidatorTest {
    private val validator = ConfigValidator()

    @Test
    fun `Validating valid config should succeed`() {
        assertDoesNotThrow {
            validator.validate(
                TestConfig(
                    settings = TestSettings(),
                    environment = TestEnvironmentConfig(
                        dockerCompose = "docker-compose.yml",
                        containers = mapOf(
                            "api-gateway" to MicroserviceContainer(
                                name = "api-gateway",
                                image = "service:latest",
                                requestConfig = RequestConfig(),
                            ),
                        ),
                        mockServers = mapOf(
                            "example-service" to MockServer(
                                name = "example-service",
                                hostname = "example.com",
                                stubs = listOf(
                                    MockServerStub(
                                        name = "request-stub-1",
                                        request = MockServerStubRequestMatcher(
                                            hostname = "example.com",
                                        ),
                                        response = MockServerStubResponse(
                                            code = 200,
                                            headers = mapOf("header1" to listOf("headerValue")),
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        }
    }

    @Test
    fun `Validating config without containers and Mock Servers should succeed`() {
        assertDoesNotThrow {
            validator.validate(
                TestConfig(
                    settings = TestSettings(),
                    environment = TestEnvironmentConfig(
                        dockerCompose = "docker-compose.yml",
                        containers = mapOf(),
                        mockServers = mapOf(),
                    )
                )
            )
        }
    }

    @Test
    fun `Validating config with duplicate hostnames should succeed`() {
        assertDoesNotThrow {
            validator.validate(
                TestConfig(
                    settings = TestSettings(),
                    environment = TestEnvironmentConfig(
                        dockerCompose = "docker-compose.yml",
                        containers = mapOf(),
                        mockServers = mapOf(
                            "example-service-1" to MockServer(
                                name = "example-service-1",
                                hostname = "example.com",
                                stubs = listOf(),
                            ),
                            "example-service-2" to MockServer(
                                name = "example-service-2",
                                hostname = "example.com",
                                stubs = listOf(),
                            ),
                        )
                    )
                )
            )
        }
    }

    @Test
    fun `Validating config with duplicate stub names should fail`() {
        assertFailsWith<ValidationException> {
            validator.validate(
                TestConfig(
                    settings = TestSettings(),
                    environment = TestEnvironmentConfig(
                        dockerCompose = "docker-compose.yml",
                        containers = mapOf(),
                        mockServers = mapOf(
                            "example-service" to MockServer(
                                name = "example-service",
                                hostname = "example.com",
                                stubs = listOf(
                                    MockServerStub(
                                        name = "request-stub",
                                        request = MockServerStubRequestMatcher(
                                            hostname = "example.com",
                                        ),
                                        response = MockServerStubResponse(code = 200)
                                    ),
                                    MockServerStub(
                                        name = "request-stub",
                                        request = MockServerStubRequestMatcher(
                                            hostname = "example.com",
                                        ),
                                        response = MockServerStubResponse(code = 200)
                                    ),
                                )
                            )
                        )
                    )
                )
            )
        }
    }
}
