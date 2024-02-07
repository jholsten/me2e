package org.jholsten.me2e.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.http.ResponseDefinition
import com.github.tomakehurst.wiremock.stubbing.ServeEvent
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import com.github.tomakehurst.wiremock.verification.LoggedRequest
import io.mockk.*
import org.jholsten.me2e.assertions.equalTo
import org.jholsten.me2e.mock.exception.VerificationException
import org.jholsten.me2e.mock.stubbing.MockServerStub
import org.jholsten.me2e.mock.verification.ExpectedRequest
import org.jholsten.me2e.request.mapper.HttpRequestMapper
import org.jholsten.me2e.request.model.HttpRequest
import org.jholsten.util.assertDoesNotThrow
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
    fun `Registering stubs when Mock Server is not initialized should throw`() {
        assertFailsWith<IllegalStateException> { server.registerStubs() }
    }

    @Test
    fun `Resetting Mock Server should reset stubs and requests`() {
        every { wireMockServer.removeServeEventsForStubsMatchingMetadata(any()) } returns mockk()
        server.initialize(wireMockServer)
        server.reset()

        val metadataMatcher = WireMock.matchingJsonPath("$.name", WireMock.equalTo(server.name))
        verify { wireMockServer.removeServeEventsForStubsMatchingMetadata(metadataMatcher) }
    }

    @Test
    fun `Resetting Mock Server when server is not initialized should throw`() {
        assertFailsWith<IllegalStateException> { server.reset() }
    }

    @Test
    fun `Retrieving requests received should return all requests for hostname`() {
        server.initialize(wireMockServer)
        every { wireMockServer.isRunning } returns true
        val request1 = mockk<LoggedRequest> {
            every { host } returns server.hostname
            every { loggedDate } returns Date(100)
        }
        val request2 = mockk<LoggedRequest> {
            every { host } returns server.hostname
            every { loggedDate } returns Date(50)
        }
        val request3 = mockk<LoggedRequest> {
            every { host } returns "another-host.de"
            every { loggedDate } returns Date(25)
        }
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
    fun `Retrieving requests received when Mock Server is not initialized should throw`() {
        assertFailsWith<IllegalStateException> { server.requestsReceived }
    }

    @Test
    fun `Retrieving requests received when Mock Server is not running should throw`() {
        every { wireMockServer.isRunning } returns false
        assertFailsWith<IllegalStateException> { server.requestsReceived }
    }

    @Test
    fun `Verifying received request should succeed`() {
        server.initialize(wireMockServer)
        every { wireMockServer.isRunning } returns true
        val request1 = mockk<LoggedRequest> {
            every { host } returns server.hostname
            every { loggedDate } returns Date(100)
        }
        val request2 = mockk<LoggedRequest> {
            every { host } returns server.hostname
            every { loggedDate } returns Date(50)
        }
        val events = listOf(
            ServeEvent(request1, mockk<StubMapping>(), mockk<ResponseDefinition>()),
            ServeEvent(request2, mockk<StubMapping>(), mockk<ResponseDefinition>()),
        )
        every { wireMockServer.allServeEvents } returns events
        mockkConstructor(ExpectedRequest::class)
        every { anyConstructed<ExpectedRequest>().matches(any(), request1) } returns true
        every { anyConstructed<ExpectedRequest>().matches(any(), request2) } returns false

        assertDoesNotThrow {
            server.verify(1, ExpectedRequest().withPath(equalTo("/some-path")))
        }
    }

    @Test
    fun `Verifying request with exact times should fail if expected request was not received`() {
        server.initialize(wireMockServer)
        every { wireMockServer.isRunning } returns true
        every { wireMockServer.allServeEvents } returns listOf()

        assertFailsWith<VerificationException> {
            server.verify(
                1, ExpectedRequest()
                    .withPath(equalTo("/some-path"))
            )
        }
        assertFailsWith<VerificationException> {
            server.verify(1, ExpectedRequest())
        }
    }

    @Test
    fun `Verifying request should fail if expected request was not received at least once`() {
        server.initialize(wireMockServer)
        every { wireMockServer.isRunning } returns true
        every { wireMockServer.allServeEvents } returns listOf()

        assertFailsWith<VerificationException> {
            server.verify(null, ExpectedRequest().withPath(equalTo("/some-path")))
        }
        assertFailsWith<VerificationException> {
            server.verify(null, ExpectedRequest())
        }
    }

    @Test
    fun `Verifying request should fail if other requests were received`() {
        server.initialize(wireMockServer)
        every { wireMockServer.isRunning } returns true
        val request1 = mockk<LoggedRequest> {
            every { host } returns server.hostname
            every { loggedDate } returns Date(100)
        }
        val request2 = mockk<LoggedRequest> {
            every { host } returns server.hostname
            every { loggedDate } returns Date(50)
        }
        val request3 = mockk<LoggedRequest> {
            every { host } returns server.hostname
            every { loggedDate } returns Date(50)
        }
        val events = listOf(
            ServeEvent(request1, mockk<StubMapping>(), mockk<ResponseDefinition>()),
            ServeEvent(request2, mockk<StubMapping>(), mockk<ResponseDefinition>()),
            ServeEvent(request3, mockk<StubMapping>(), mockk<ResponseDefinition>()),
        )
        every { wireMockServer.allServeEvents } returns events
        mockkConstructor(ExpectedRequest::class)
        every { anyConstructed<ExpectedRequest>().matches(any(), request1) } returns true
        every { anyConstructed<ExpectedRequest>().matches(any(), request2) } returns true
        every { anyConstructed<ExpectedRequest>().matches(any(), request3) } returns false

        val request1Verification = ExpectedRequest().withPath(equalTo("/some-path"))
        val request2Verification = ExpectedRequest().withPath(equalTo("/other-path"))
        assertDoesNotThrow { server.verify(null, request1Verification) }
        assertDoesNotThrow { server.verify(null, request2Verification) }
        assertFailsWith<VerificationException> { server.verify(null, request1Verification.andNoOther()) }
        assertFailsWith<VerificationException> { server.verify(null, request2Verification.andNoOther()) }
    }

    @Test
    fun `Verifying request should fail if Mock Server is not initialized`() {
        assertFailsWith<IllegalStateException> { server.verify(null, ExpectedRequest()) }
    }

    @Test
    fun `Verifying request should fail if Mock Server is not running`() {
        server.initialize(wireMockServer)
        every { wireMockServer.isRunning } returns false

        assertFailsWith<IllegalStateException> { server.verify(null, ExpectedRequest()) }
    }
}
