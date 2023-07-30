package org.jholsten.me2e.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.common.FatalStartupException
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.mockk.*
import org.jholsten.me2e.container.exception.ServiceStartupException
import org.jholsten.me2e.container.healthcheck.exception.ServiceNotHealthyException
import org.jholsten.me2e.mock.parser.YamlMockServerStubParser
import org.jholsten.me2e.mock.stubbing.MockServerStub
import org.jholsten.me2e.request.mapper.HttpRequestMapper
import org.jholsten.util.RecursiveComparison
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.lang.RuntimeException


internal class MockServerTest {

    private val httpRequestMapper = mockk<HttpRequestMapper>()

    @BeforeEach
    fun beforeEach() {
        mockkConstructor(YamlMockServerStubParser::class)
        mockkConstructor(WireMockServer::class)
        mockkConstructor(WireMockConfiguration::class)

        mockkObject(HttpRequestMapper.Companion)
        every { HttpRequestMapper.INSTANCE } returns httpRequestMapper
    }

    @AfterEach
    fun afterEach() {
        unmockkAll()
    }

    @Test
    fun `Constructing mock server should instantiate wire mock server and stubs`() {
        val stub = mockParsingStub()
        val server = MockServer(name = "service", port = 9000, stubs = listOf("request_stub.json"))

        assertEquals(1, server.stubs.size)
        RecursiveComparison.assertEquals(stub, server.stubs.first())
        verify { stub.register(any()) }
        verify { constructedWith<WireMockConfiguration>().port(9000) }
    }

    @Test
    fun `Starting mock server should start wire mock`() {
        every { anyConstructed<WireMockServer>().start() } just runs
        every { anyConstructed<WireMockServer>().isRunning } returns true

        val server = MockServer(name = "service", port = 9000, stubs = listOf())
        assertDoesNotThrow { server.start() }

        assertTrue(server.isRunning())
        verify { anyConstructed<WireMockServer>().start() }
        verify { anyConstructed<WireMockServer>().isRunning }
    }

    @Test
    fun `Starting mock server should fail when wire mock does not start`() {
        every { anyConstructed<WireMockServer>().start() } just runs
        every { anyConstructed<WireMockServer>().isRunning } returns false

        val server = MockServer(name = "service", port = 9000, stubs = listOf())
        assertThrowsExactly(ServiceNotHealthyException::class.java) { server.start() }

        assertFalse(server.isRunning())
        verify { anyConstructed<WireMockServer>().start() }
        verify { anyConstructed<WireMockServer>().isRunning }
    }

    @Test
    fun `Starting mock server should fail when wire mock fails`() {
        every { anyConstructed<WireMockServer>().start() } throws FatalStartupException(RuntimeException())

        val server = MockServer(name = "service", port = 9000, stubs = listOf())
        assertThrowsExactly(ServiceStartupException::class.java) { server.start() }

        verify { anyConstructed<WireMockServer>().start() }
    }

    @Test
    fun `Stopping mock server should stop wire mock`() {
        every { anyConstructed<WireMockServer>().stop() } just runs

        val server = MockServer(name = "service", port = 9000, stubs = listOf())
        assertDoesNotThrow { server.stop() }

        verify { anyConstructed<WireMockServer>().stop() }
    }

    private fun mockParsingStub(): MockServerStub {
        val stub = mockk<MockServerStub>()
        every { anyConstructed<YamlMockServerStubParser>().parseFile(any()) } returns stub
        every { stub.register(any()) } just runs

        return stub
    }
}
