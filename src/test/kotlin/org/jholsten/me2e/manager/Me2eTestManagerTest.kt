package org.jholsten.me2e.manager

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.jholsten.me2e.config.model.ConfigFormat
import org.jholsten.me2e.config.model.TestConfig
import org.jholsten.me2e.config.parser.YamlConfigParser
import org.jholsten.me2e.container.Container
import org.jholsten.me2e.container.microservice.MicroserviceContainer
import org.jholsten.me2e.container.model.ContainerType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class Me2eTestManagerTest {
    
    @Test
    fun `Using builder should create Test Manager instance`() {
        val config = testConfig()
        val yamlConfigParser = mockk<YamlConfigParser>()
        every { yamlConfigParser.parseFile(any()) } returns config
        val format = mockk<ConfigFormat>()
        every { format.parser } returns yamlConfigParser
        
        val expectedContainerNames = listOf("gateway-service", "database")
        val expectedMicroservices = mapOf("gateway-service" to config.containers["gateway-service"])
        
        val manager = Me2eTestManager.Builder()
            .withFile("any-file", format)
            .build()
        
        assertEquals(expectedContainerNames, manager.containerNames)
        assertEquals(expectedMicroservices, manager.microservices)
        verify { yamlConfigParser.parseFile("any-file") }
    }
    
    @Test
    fun `Using builder without specifying config should fail`() {
        assertThrowsExactly(IllegalArgumentException::class.java) { Me2eTestManager.Builder().build() }
    }
    
    private fun testConfig(): TestConfig {
        return TestConfig(
            containers = mapOf(
                "gateway-service" to MicroserviceContainer(
                    name = "gateway-service",
                    image = "service:latest",
                ),
                "database" to Container(
                    name = "database",
                    type = ContainerType.DATABASE,
                    image = "postgres:12",
                )
            )
        )
    }
}
