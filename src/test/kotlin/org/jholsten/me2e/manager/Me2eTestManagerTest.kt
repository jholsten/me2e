package org.jholsten.me2e.manager

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.jholsten.me2e.config.model.ConfigFormat
import org.jholsten.me2e.config.model.RequestConfig
import org.jholsten.me2e.config.model.TestConfig
import org.jholsten.me2e.config.model.TestEnvironmentConfig
import org.jholsten.me2e.config.parser.YamlConfigParser
import org.jholsten.me2e.container.Container
import org.jholsten.me2e.container.microservice.MicroserviceContainer
import org.jholsten.me2e.container.model.ContainerType
import kotlin.test.*

internal class Me2eTestManagerTest {

    private val yamlConfigParser = mockk<YamlConfigParser>()
    private val yamlFormat = mockk<ConfigFormat>()

    @Test
    fun `Using builder should create Test Manager instance`() {
        val config = testConfig()
        every { yamlConfigParser.parseFile(any()) } returns config
        every { yamlFormat.parser } returns yamlConfigParser

        val expectedContainerNames = listOf("gateway-service", "database")
        val expectedMicroservices = mapOf("gateway-service" to config.environment.containers["gateway-service"])

        val manager = Me2eTestManager.Builder()
            .withFile("any-file", yamlFormat)
            .build()

        assertEquals(expectedContainerNames, manager.containerNames)
        assertEquals(expectedMicroservices, manager.microservices)
        verify { yamlConfigParser.parseFile("any-file") }
    }

    @Test
    fun `Using builder without specifying config should fail`() {
        assertFailsWith<IllegalArgumentException> { Me2eTestManager.Builder().build() }
    }

    private fun testConfig(): TestConfig {
        return TestConfig(
            environment = TestEnvironmentConfig(
                containers = mapOf(
                    "gateway-service" to MicroserviceContainer(
                        name = "gateway-service",
                        image = "service:latest",
                        requestConfig = RequestConfig(),
                    ),
                    "database" to Container(
                        name = "database",
                        type = ContainerType.DATABASE,
                        image = "postgres:12",
                    )
                )
            ),
        )
    }
}
