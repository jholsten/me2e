package org.jholsten.me2e.container

import com.github.dockerjava.api.command.LogContainerCmd
import com.github.dockerjava.api.model.ContainerPort
import com.github.dockerjava.api.model.Container as DockerContainer
import io.mockk.*
import org.jholsten.me2e.container.model.ContainerType
import org.jholsten.me2e.report.result.ReportDataAggregator
import org.jholsten.util.RecursiveComparison
import org.testcontainers.containers.ContainerState
import kotlin.test.*

internal class ContainerTest {

    private val dockerContainer = mockk<DockerContainer>()
    private val dockerContainerState = mockk<ContainerState>()

    val container = Container(
        name = "backend",
        type = ContainerType.MISC,
        image = "backend:latest",
        ports = Container.ContainerPortList(
            listOf(
                Container.ContainerPort(12345, 8000),
                Container.ContainerPort(12346, 8001),
            )
        ),
    )

    @BeforeTest
    fun beforeTest() {
        every { dockerContainerState.dockerClient } returns mockk {
            every { logContainerCmd(any()) } returns mockk<LogContainerCmd> {
                every { withFollowStream(any()) } returns this
                every { withSince(any()) } returns this
                every { withStdOut(any()) } returns this
                every { withStdErr(any()) } returns this
                every { exec(any()) } returns mockk()
            }
        }
        every { dockerContainerState.containerId } returns "container-id"
        mockkObject(ReportDataAggregator)
        every { ReportDataAggregator.onContainerStarted(any()) } just runs
    }

    @AfterTest
    fun afterTest() {
        unmockkAll()
    }

    @Test
    fun `Initializing container should map container ports`() {
        val container = Container(
            name = "backend",
            type = ContainerType.MISC,
            image = "backend:latest",
            ports = Container.ContainerPortList(
                listOf(
                    Container.ContainerPort(12345),
                    Container.ContainerPort(12347),
                    Container.ContainerPort(12348),
                )
            ),
        )

        every { dockerContainer.getPorts() } returns arrayOf(
            ContainerPort().withPrivatePort(12345).withPublicPort(8000),
            ContainerPort().withPrivatePort(12346).withPublicPort(8001),
            ContainerPort().withPrivatePort(12347).withPublicPort(null),
            ContainerPort().withPrivatePort(null).withPublicPort(8002),
        )

        container.initialize(dockerContainer, dockerContainerState)

        val expected = listOf(
            Container.ContainerPort(12345, 8000),
            Container.ContainerPort(12347, null),
            Container.ContainerPort(12348, null),
        )
        RecursiveComparison.assertEquals(expected, container.ports)
    }

    @Test
    fun `Finding by internal port should return the correct port`() {
        val ports = Container.ContainerPortList(
            listOf(
                Container.ContainerPort(12345, 8000),
                Container.ContainerPort(12346, null),
            )
        )

        assertEquals(ports[0], ports.findByInternalPort(12345))
        assertEquals(ports[1], ports.findByInternalPort(12346))
        assertNull(ports.findByInternalPort(1111))
    }

    @Test
    fun `Finding first exposed port should return the correct port`() {
        val ports = Container.ContainerPortList(
            listOf(
                Container.ContainerPort(12345, null),
                Container.ContainerPort(12346, 8000),
            )
        )

        assertEquals(ports[1], ports.findFirstExposed())
    }

    @Test
    fun `Finding first exposed port should return null if no port is exposed`() {
        val ports = Container.ContainerPortList(
            listOf(
                Container.ContainerPort(12345, null),
                Container.ContainerPort(12346, null),
            )
        )

        assertNull(ports.findFirstExposed())
    }
}
