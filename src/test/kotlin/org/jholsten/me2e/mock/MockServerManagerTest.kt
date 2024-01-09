package org.jholsten.me2e.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.common.FatalStartupException
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.http.ResponseDefinition
import com.github.tomakehurst.wiremock.stubbing.ServeEvent
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import com.github.tomakehurst.wiremock.verification.LoggedRequest
import io.mockk.*
import org.jholsten.me2e.container.exception.ServiceStartupException
import org.jholsten.me2e.container.exception.ServiceNotHealthyException
import org.jholsten.me2e.mock.stubbing.MockServerStubNotMatchedRenderer
import org.jholsten.me2e.request.mapper.HttpRequestMapper
import org.jholsten.me2e.request.model.HttpRequest
import org.jholsten.me2e.utils.PortUtils
import java.util.*
import kotlin.test.*

internal class MockServerManagerTest {

    private val httpRequestMapper = mockk<HttpRequestMapper>()

    @BeforeTest
    fun beforeTest() {
        mockkConstructor(WireMockServer::class)
        mockkConstructor(WireMockConfiguration::class)
        mockkConstructor(MockServerStubNotMatchedRenderer::class)

        mockkObject(HttpRequestMapper.Companion)
        every { HttpRequestMapper.INSTANCE } returns httpRequestMapper

        mockkObject(PortUtils.Companion)
    }

    @AfterTest
    fun afterTest() {
        unmockkAll()
    }

    @Test
    fun `Constructing mock server manager should instantiate wire mock server`() {
        val configMock = spyk<WireMockConfiguration>()
        every { constructedWith<WireMockConfiguration>().port(any()) } returns configMock
        every { configMock.httpsPort(any()) } returns configMock

        val manager = mockServerManager()

        verify { constructedWith<WireMockConfiguration>().port(80) }
        verify { configMock.httpsPort(443) }
        for (server in manager.mockServers.values) {
            verify { server.initialize(any()) }
        }
    }

    @Test
    fun `Starting mock server manager should start wire mock`() {
        every { PortUtils.isPortAvailable(any()) } returns true
        every { anyConstructed<WireMockServer>().start() } just runs
        every { anyConstructed<WireMockServer>().isRunning } returns false andThen true

        val manager = MockServerManager(mapOf())
        manager.start()

        verify { PortUtils.isPortAvailable(80) }
        verify { PortUtils.isPortAvailable(443) }
        verify { anyConstructed<WireMockServer>().start() }
        verify { anyConstructed<WireMockServer>().isRunning }
    }

    @Test
    fun `Starting mock server manager should fail if wire mock is already running`() {
        every { anyConstructed<WireMockServer>().isRunning } returns true

        val manager = MockServerManager(mapOf())
        assertFailsWith<IllegalStateException> { manager.start() }
    }

    @Test
    fun `Starting mock server manager should fail if HTTP port is in use`() {
        every { PortUtils.isPortAvailable(80) } returns false
        every { PortUtils.isPortAvailable(443) } returns true
        every { anyConstructed<WireMockServer>().isRunning } returns false

        val manager = MockServerManager(mapOf())
        assertFailsWith<ServiceStartupException> { manager.start() }
    }

    @Test
    fun `Starting mock server manager should fail if HTTPS port is in use`() {
        every { PortUtils.isPortAvailable(80) } returns true
        every { PortUtils.isPortAvailable(443) } returns false
        every { anyConstructed<WireMockServer>().isRunning } returns false

        val manager = MockServerManager(mapOf())
        assertFailsWith<ServiceStartupException> { manager.start() }
    }

    @Test
    fun `Starting mock server manager should fail when wire mock does not start`() {
        every { PortUtils.isPortAvailable(any()) } returns true
        every { anyConstructed<WireMockServer>().start() } just runs
        every { anyConstructed<WireMockServer>().isRunning } returns false

        val manager = MockServerManager(mapOf())
        assertFailsWith<ServiceNotHealthyException> { manager.start() }

        verify { PortUtils.isPortAvailable(80) }
        verify { PortUtils.isPortAvailable(443) }
        verify { anyConstructed<WireMockServer>().start() }
        verify { anyConstructed<WireMockServer>().isRunning }
    }

    @Test
    fun `Starting mock server manager should fail when wire mock fails`() {
        every { PortUtils.isPortAvailable(any()) } returns true
        every { anyConstructed<WireMockServer>().start() } throws FatalStartupException(RuntimeException())
        every { anyConstructed<WireMockServer>().isRunning } returns false

        val manager = MockServerManager(mapOf())
        val e = assertFailsWith<ServiceStartupException> { manager.start() }

        assertContains(e.message!!, "Mock server could not be started")
        verify { anyConstructed<WireMockServer>().start() }
    }

    @Test
    fun `Stopping mock server should stop wire mock`() {
        every { anyConstructed<WireMockServer>().stop() } just runs
        every { anyConstructed<WireMockServer>().isRunning } returns true

        val manager = MockServerManager(mapOf())
        manager.stop()

        verify { anyConstructed<WireMockServer>().stop() }
    }

    @Test
    fun `Stopping mock server should fail if wire mock is not running`() {
        every { anyConstructed<WireMockServer>().isRunning } returns false

        val manager = MockServerManager(mapOf())
        assertFailsWith<IllegalStateException> { manager.stop() }
    }

    @Test
    fun `Retrieving requests received should return all requests`() {
        every { anyConstructed<WireMockServer>().isRunning } returns true
        val request1 = mockedLoggedRequest("host1.com", Date(100))
        val request2 = mockedLoggedRequest("host1.com", Date(50))
        val request3 = mockedLoggedRequest("another-host.de", Date(25))
        val events = listOf(
            ServeEvent(request1, mockk<StubMapping>(), mockk<ResponseDefinition>()),
            ServeEvent(request2, mockk<StubMapping>(), mockk<ResponseDefinition>()),
            ServeEvent(request3, mockk<StubMapping>(), mockk<ResponseDefinition>()),
        )
        every { anyConstructed<WireMockServer>().allServeEvents } returns events
        val mappedRequest1 = mockk<HttpRequest>()
        val mappedRequest2 = mockk<HttpRequest>()
        val mappedRequest3 = mockk<HttpRequest>()
        every { httpRequestMapper.toInternalDto(request1) } returns mappedRequest1
        every { httpRequestMapper.toInternalDto(request2) } returns mappedRequest2
        every { httpRequestMapper.toInternalDto(request3) } returns mappedRequest3

        val manager = MockServerManager(mapOf())
        val requests = manager.requestsReceived

        assertEquals(3, requests.size)
        assertContains(requests, mappedRequest1)
        assertContains(requests, mappedRequest2)
        assertContains(requests, mappedRequest3)
        assertEquals(listOf(mappedRequest3, mappedRequest2, mappedRequest1), requests)
    }

    @Test
    fun `Registering all stubs should register stubs of all mock servers`() {
        val manager = mockServerManager()
        manager.registerAllStubs()

        for (service in manager.mockServers) {
            verify { service.value.registerStubs() }
        }
    }

    @Test
    fun `Resetting should reset at all mock servers and wire mock`() {
        every { anyConstructed<WireMockServer>().resetAll() } just runs

        val manager = mockServerManager()
        manager.resetAll()

        for (service in manager.mockServers) {
            verify { service.value.reset() }
        }
        verify { anyConstructed<WireMockServer>().resetAll() }
    }

    private fun mockServerManager(): MockServerManager {
        val server1 = mockk<MockServer> {
            every { registerStubs() } just runs
            every { reset() } just runs
            every { initialize(any()) } just runs
        }
        val server2 = mockk<MockServer> {
            every { registerStubs() } just runs
            every { reset() } just runs
            every { initialize(any()) } just runs
        }
        return MockServerManager(mapOf("service-1" to server1, "service-2" to server2))
    }

    private fun mockedLoggedRequest(hostname: String, loggedDate: Date): LoggedRequest {
        val request = mockk<LoggedRequest>()
        every { request.host } returns hostname
        every { request.loggedDate } returns loggedDate
        return request
    }
}
