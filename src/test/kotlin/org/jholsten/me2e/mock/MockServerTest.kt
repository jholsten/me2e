package org.jholsten.me2e.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.http.ResponseDefinition
import com.github.tomakehurst.wiremock.stubbing.ServeEvent
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import com.github.tomakehurst.wiremock.verification.LoggedRequest
import io.mockk.*
import org.jholsten.me2e.mock.stubbing.MockServerStub
import org.jholsten.me2e.request.mapper.HttpRequestMapper
import org.jholsten.me2e.request.model.HttpRequest
import java.util.*
import kotlin.test.*


internal class MockServerTest {

    private val httpRequestMapper = mockk<HttpRequestMapper>()

    private val wireMockServer = mockk<WireMockServer>()

    private val server = MockServer(
        name = "service",
        hostname = "example.com",
        stubs = listOf(mockk<MockServerStub>(), mockk<MockServerStub>()),
    )

    @BeforeTest
    fun beforeTest() {
        for (stub in server.stubs) {
            every { stub.registerAt(any(), any()) } just runs
        }
        mockkObject(HttpRequestMapper.Companion)
        every { HttpRequestMapper.INSTANCE } returns httpRequestMapper
    }

    @AfterTest
    fun afterTest() {
        unmockkAll()
    }

    @Test
    fun `Registering stubs should register all stubs`() {
        server.initialize(wireMockServer)
        server.registerStubs()

        for (stub in server.stubs) {
            verify { stub.registerAt(server.name, wireMockServer) }
        }
    }

    @Test
    fun `Registering stubs when mock server is not initialized should throw`() {
        assertFailsWith<IllegalStateException> { server.registerStubs() }
    }

    @Test
    fun `Resetting mock server should reset stubs and requests`() {
        every { wireMockServer.removeStubsByMetadata(any()) } just runs
        every { wireMockServer.removeServeEventsForStubsMatchingMetadata(any()) } returns mockk()
        server.initialize(wireMockServer)
        server.reset()

        val metadataMatcher = WireMock.matchingJsonPath("$.name", WireMock.equalTo(server.name))
        verify { wireMockServer.removeStubsByMetadata(metadataMatcher) }
        verify { wireMockServer.removeServeEventsForStubsMatchingMetadata(metadataMatcher) }
    }

    @Test
    fun `Resetting mock server when server is not initialized should throw`() {
        assertFailsWith<IllegalStateException> { server.reset() }
    }

    @Test
    fun `Retrieving requests received should return all requests for hostname`() {
        server.initialize(wireMockServer)
        every { wireMockServer.isRunning } returns true
        val request1 = mockedLoggedRequest(server.hostname, Date(100))
        val request2 = mockedLoggedRequest(server.hostname, Date(50))
        val request3 = mockedLoggedRequest("another-host.de", Date(25))
        val events = listOf(
            ServeEvent(request1, mockk<StubMapping>(), mockk<ResponseDefinition>()),
            ServeEvent(request2, mockk<StubMapping>(), mockk<ResponseDefinition>()),
            ServeEvent(request3, mockk<StubMapping>(), mockk<ResponseDefinition>()),
        )
        every { wireMockServer.allServeEvents } returns events
        val mappedRequest1 = mockk<HttpRequest>()
        val mappedRequest2 = mockk<HttpRequest>()
        val mappedRequest3 = mockk<HttpRequest>()
        every { httpRequestMapper.toInternalDto(request1) } returns mappedRequest1
        every { httpRequestMapper.toInternalDto(request2) } returns mappedRequest2
        every { httpRequestMapper.toInternalDto(request3) } returns mappedRequest3

        val requests = server.requestsReceived

        assertEquals(2, requests.size)
        assertContains(requests, mappedRequest1)
        assertContains(requests, mappedRequest2)
        assertEquals(listOf(mappedRequest2, mappedRequest1), requests)
    }

    @Test
    fun `Retrieving requests received when mock server is not initialized should throw`() {
        assertFailsWith<IllegalStateException> { server.requestsReceived }
    }

    @Test
    fun `Retrieving requests received when mock server is not running should throw`() {
        every { wireMockServer.isRunning } returns false
        assertFailsWith<IllegalStateException> { server.requestsReceived }
    }

    private fun mockedLoggedRequest(hostname: String, loggedDate: Date): LoggedRequest {
        val request = mockk<LoggedRequest>()
        every { request.host } returns hostname
        every { request.loggedDate } returns loggedDate
        return request
    }
}
