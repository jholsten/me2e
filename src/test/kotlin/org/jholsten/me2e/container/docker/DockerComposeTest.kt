package org.jholsten.me2e.container.docker

import io.mockk.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.testcontainers.containers.wait.strategy.WaitAllStrategy
import org.testcontainers.containers.wait.strategy.WaitStrategy
import java.io.File
import kotlin.test.*
import org.testcontainers.containers.ComposeContainer as DockerComposeV2
import org.testcontainers.containers.DockerComposeContainer as DockerComposeV1

internal class DockerComposeTest {

    private val dockerComposeFile: File = org.jholsten.me2e.parsing.utils.FileUtils.getResourceAsFile("docker-compose-parsing-test.yml")
    private val waitStrategy: WaitStrategy = mockk<WaitAllStrategy>()

    @BeforeTest
    fun beforeTest() {
        mockkConstructor(DockerCompose.KDockerComposeV1::class)
        mockkConstructor(DockerComposeV2::class)
        every { anyConstructed<DockerCompose.KDockerComposeV1>().start() } just runs
        every { anyConstructed<DockerComposeV2>().start() } just runs
    }

    @AfterTest
    fun afterTest() {
        unmockkAll()
    }

    @Test
    fun `Using Docker-Compose V1 should use V1 implementation`() {
        val builder = DockerCompose.Builder("identifier", dockerComposeFile, version = DockerComposeVersion.V1)

        builder.withLocalCompose(true)
            .withBuild(true)
            .withRemoveImages(DockerComposeRemoveImagesStrategy.ALL)
            .withRemoveVolumes(true)
        builder.waitingFor("service", waitStrategy)
        val compose = builder.build()
        compose.getContainerByServiceName("service")
        compose.start()

        verifyAll(shouldHaveUsedV1 = true, shouldHaveUsedV2 = false)
    }

    @Test
    fun `Using Docker-Compose V2 should use V2 implementation`() {
        val builder = DockerCompose.Builder("identifier", dockerComposeFile, version = DockerComposeVersion.V2)

        builder.withLocalCompose(true)
            .withBuild(true)
            .withRemoveImages(DockerComposeRemoveImagesStrategy.ALL)
            .withRemoveVolumes(true)
        builder.waitingFor("service", waitStrategy)
        val compose = builder.build()
        compose.getContainerByServiceName("service")
        compose.start()

        verifyAll(shouldHaveUsedV1 = false, shouldHaveUsedV2 = true)
    }

    @ParameterizedTest(name = "[{index}] {0} should be mapped to {1}")
    @CsvSource(
        "NONE, null",
        "ALL, ALL",
        "LOCAL, LOCAL",
        nullValues = ["null"]
    )
    fun `Docker compose remove images strategy should be mapped to V1 enum`(
        strategy: DockerComposeRemoveImagesStrategy,
        v1Enum: DockerComposeV1.RemoveImages?
    ) {
        val compose = DockerCompose.Builder("identifier", dockerComposeFile, version = DockerComposeVersion.V1)

        compose.withRemoveImages(strategy)

        verify { anyConstructed<DockerCompose.KDockerComposeV1>().withRemoveImages(v1Enum) }
    }

    @ParameterizedTest(name = "[{index}] {0} should be mapped to {1}")
    @CsvSource(
        "NONE, null",
        "ALL, ALL",
        "LOCAL, LOCAL",
        nullValues = ["null"]
    )
    fun `Docker compose remove images strategy should be mapped to V2 enum`(
        strategy: DockerComposeRemoveImagesStrategy,
        v2Enum: DockerComposeV2.RemoveImages?
    ) {
        val compose = DockerCompose.Builder("identifier", dockerComposeFile, version = DockerComposeVersion.V2)

        compose.withRemoveImages(strategy)

        verify { anyConstructed<DockerComposeV2>().withRemoveImages(v2Enum) }
    }

    private fun verifyAll(shouldHaveUsedV1: Boolean, shouldHaveUsedV2: Boolean) {
        val expectedV1 = if (shouldHaveUsedV1) 1 else 0
        verify(exactly = expectedV1) { anyConstructed<DockerCompose.KDockerComposeV1>().withLocalCompose(true) }
        verify(exactly = expectedV1) { anyConstructed<DockerCompose.KDockerComposeV1>().withBuild(true) }
        verify(exactly = expectedV1) { anyConstructed<DockerCompose.KDockerComposeV1>().withRemoveImages(DockerComposeV1.RemoveImages.ALL) }
        verify(exactly = expectedV1) { anyConstructed<DockerCompose.KDockerComposeV1>().withRemoveVolumes(true) }
        verify(exactly = expectedV1) { anyConstructed<DockerCompose.KDockerComposeV1>().waitingFor("service", waitStrategy) }
        verify(exactly = expectedV1) { anyConstructed<DockerCompose.KDockerComposeV1>().getContainerByServiceName("service") }
        verify(exactly = expectedV1) { anyConstructed<DockerCompose.KDockerComposeV1>().start() }

        val expectedV2 = if (shouldHaveUsedV2) 1 else 0
        verify(exactly = expectedV2) { anyConstructed<DockerComposeV2>().withLocalCompose(true) }
        verify(exactly = expectedV2) { anyConstructed<DockerComposeV2>().withBuild(true) }
        verify(exactly = expectedV2) { anyConstructed<DockerComposeV2>().withRemoveImages(DockerComposeV2.RemoveImages.ALL) }
        verify(exactly = expectedV2) { anyConstructed<DockerComposeV2>().withRemoveVolumes(true) }
        verify(exactly = expectedV2) { anyConstructed<DockerComposeV2>().waitingFor("service", waitStrategy) }
        verify(exactly = expectedV2) { anyConstructed<DockerComposeV2>().getContainerByServiceName("service") }
        verify(exactly = expectedV2) { anyConstructed<DockerComposeV2>().start() }
    }
}
