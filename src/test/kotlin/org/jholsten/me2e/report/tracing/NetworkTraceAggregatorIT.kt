package org.jholsten.me2e.report.tracing

import org.jholsten.me2e.Me2eTestConfigScanner
import org.jholsten.me2e.container.ContainerManager
import org.jholsten.me2e.container.microservice.MicroserviceContainer
import org.jholsten.me2e.mock.MockServerManager
import org.jholsten.me2e.parsing.utils.FileUtils
import org.jholsten.me2e.report.logs.model.ServiceSpecification
import org.jholsten.me2e.report.result.model.FinishedTestResult
import org.jholsten.me2e.report.result.model.TestResult
import org.jholsten.me2e.report.tracing.model.AggregatedNetworkTrace
import org.jholsten.me2e.report.tracing.model.NetworkNodeSpecification
import org.jholsten.me2e.request.model.HttpRequestBody
import org.jholsten.me2e.request.model.MediaType
import org.jholsten.me2e.request.model.RelativeUrl
import org.jholsten.util.RecursiveComparison
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import java.time.Instant
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

internal class NetworkTraceAggregatorIT {

    companion object {
        private val networkTraceAggregator = NetworkTraceAggregator()
        private val configAnnotation = Me2eTestConfigScanner.findFirstTestConfigAnnotation()!!
        private val config = configAnnotation.format.parser.parseFile(configAnnotation.config)

        private val containerManager: ContainerManager = ContainerManager(
            dockerComposeFile = FileUtils.getResourceAsFile(config.environment.dockerCompose),
            dockerConfig = config.settings.docker,
            containers = config.environment.containers,
        )

        private val mockServerManager: MockServerManager = MockServerManager(
            mockServers = config.environment.mockServers,
            mockServerConfig = config.settings.mockServers,
        )

        private val startTime = Instant.now()

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            mockServerManager.start()
            containerManager.start()
            for (container in containerManager.containers.values) {
                networkTraceAggregator.onContainerStarted(container, ServiceSpecification(name = container.name))
            }
        }

        @AfterAll
        @JvmStatic
        fun afterAll() {
            mockServerManager.stop()
            containerManager.stop()
        }
    }

    private val backendApi: MicroserviceContainer = containerManager.containers["backend-api"] as MicroserviceContainer

    @Test
    fun `Packets from all networks should be aggregated to traces`() {
        val test =
            FinishedTestResult(
                testId = "TestA",
                source = "org.jholsten.me2e.TestA",
                path = listOf(),
                parentId = null,
                children = listOf(),
                status = TestResult.Status.SUCCESSFUL,
                startTime = startTime,
                endTime = Instant.now().plusSeconds(10),
                numberOfTests = 1,
                numberOfFailures = 0,
                numberOfSkipped = 0,
                numberOfAborted = 0,
                displayName = "Test A",
                tags = setOf(),
                reportEntries = listOf(),
                logs = listOf(),
                stats = listOf(),
                throwable = null,
            )

        backendApi.post(RelativeUrl("/search"), HttpRequestBody(content = "{\"id\": 123}", MediaType.JSON_UTF8))

        networkTraceAggregator.collectPackets(listOf(test))

        assertEquals(2, test.traces.size)
        val expectedRequest = expectedRequestPacket(
            version = "HTTP/1.1",
            "/search",
            method = "POST",
            payload = "{\"id\": 123}",
        )
        val expectedResponse = expectedResponsePacket(
            version = "HTTP/1.1",
            200,
            statusCodeDescription = "OK",
            payload = "{\"id\":123,\"items\":[{\"name\":\"A\",\"value\":42},{\"name\":\"B\",\"value\":1}]}",
        )
        val first = test.traces[0]
        assertTraceAsExpected(
            first,
            parentId = null,
            streamId = null,
            client = NetworkNodeSpecification(
                nodeType = NetworkNodeSpecification.NodeType.SERVICE,
                ipAddress = "ANY",
                specification = ServiceSpecification(name = "Test Runner"),
            ),
            server = NetworkNodeSpecification(
                nodeType = NetworkNodeSpecification.NodeType.SERVICE,
                ipAddress = "ANY",
                specification = ServiceSpecification(name = "backend-api"),
            ),
            request = expectedRequest,
            response = expectedResponse
        )

        val second = test.traces[1]
        assertTraceAsExpected(
            second,
            parentId = first.id,
            streamId = first.streamId,
            client = NetworkNodeSpecification(
                nodeType = NetworkNodeSpecification.NodeType.SERVICE,
                ipAddress = "ANY",
                specification = ServiceSpecification(name = "backend-api"),
            ),
            server = NetworkNodeSpecification(
                nodeType = NetworkNodeSpecification.NodeType.MOCK_SERVER,
                ipAddress = "ANY",
                specification = ServiceSpecification(name = "payment-service"),
            ),
            request = expectedRequest,
            response = expectedResponse
        )
    }

    private fun expectedRequestPacket(
        version: String,
        uri: String,
        method: String,
        payload: Any?,
    ): AggregatedNetworkTrace.RequestPacket {
        return AggregatedNetworkTrace.RequestPacket(
            number = -1,
            timestamp = Instant.now(),
            sourceIp = "ANY",
            sourcePort = -1,
            destinationIp = "ANY",
            destinationPort = -1,
            version = version,
            uri = uri,
            method = method,
            headers = mapOf(),
            payload = payload,
        )
    }

    private fun expectedResponsePacket(
        version: String,
        statusCode: Int,
        statusCodeDescription: String,
        payload: Any?,
    ): AggregatedNetworkTrace.ResponsePacket {
        return AggregatedNetworkTrace.ResponsePacket(
            number = -1,
            timestamp = Instant.now(),
            sourceIp = "ANY",
            sourcePort = -1,
            destinationIp = "ANY",
            destinationPort = -1,
            version = version,
            statusCode = statusCode,
            statusCodeDescription = statusCodeDescription,
            headers = mapOf(),
            payload = payload,
        )
    }

    private fun assertTraceAsExpected(
        trace: AggregatedNetworkTrace,
        parentId: UUID?,
        streamId: UUID?,
        client: NetworkNodeSpecification,
        server: NetworkNodeSpecification,
        request: AggregatedNetworkTrace.RequestPacket,
        response: AggregatedNetworkTrace.ResponsePacket,
    ) {
        assertEquals(parentId, trace.parentId)
        if (streamId != null) {
            assertEquals(streamId, trace.streamId)
        }
        RecursiveComparison.assertEquals(client, trace.client, fieldsToIgnore = listOf("id", "color", "ipAddress"))
        RecursiveComparison.assertEquals(server, trace.server, fieldsToIgnore = listOf("id", "color", "ipAddress"))
        RecursiveComparison.assertEquals(
            request,
            trace.request,
            fieldsToIgnore = listOf("number", "timestamp", "sourceIp", "sourcePort", "destinationIp", "destinationPort", "headers")
        )
        RecursiveComparison.assertEquals(
            response,
            trace.response,
            fieldsToIgnore = listOf("number", "timestamp", "sourceIp", "sourcePort", "destinationIp", "destinationPort", "headers")
        )
    }
}
