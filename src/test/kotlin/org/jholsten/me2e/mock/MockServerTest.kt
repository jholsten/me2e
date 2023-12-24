package org.jholsten.me2e.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.mockk.*
import org.jholsten.me2e.mock.parser.YamlMockServerStubParser
import org.jholsten.me2e.request.mapper.HttpRequestMapper
import org.jholsten.util.RecursiveComparison
import kotlin.test.*


// TODO
internal class MockServerTest {

    private val httpRequestMapper = mockk<HttpRequestMapper>()

    @BeforeTest
    fun beforeTest() {
        mockkConstructor(YamlMockServerStubParser::class)
        mockkConstructor(WireMockServer::class)
        mockkConstructor(WireMockConfiguration::class)

        mockkObject(HttpRequestMapper.Companion)
        every { HttpRequestMapper.INSTANCE } returns httpRequestMapper
    }

    @AfterTest
    fun afterTest() {
        unmockkAll()
    }

//    @Test
//    fun `Constructing mock server should instantiate wire mock server and stubs`() {
//        val stub = mockParsingStub()
//        val server = MockServer(name = "service", hostname = 9000, stubs = listOf("request_stub.json"))
//
//        assertEquals(1, server.stubs.size)
//        RecursiveComparison.assertEquals(stub, server.stubs.first())
//        verify { stub.registerAt(any()) }
//        verify { constructedWith<WireMockConfiguration>().port(9000) }
//    }
//
//    @Test
//    fun `Starting mock server should start wire mock`() {
//        every { anyConstructed<WireMockServer>().start() } just runs
//        every { anyConstructed<WireMockServer>().isRunning } returns true
//
//        val server = MockServer(name = "service", hostname = 9000, stubs = listOf())
//        assertDoesNotThrow { server.start() }
//
//        assertTrue(server.isRunning())
//        verify { anyConstructed<WireMockServer>().start() }
//        verify { anyConstructed<WireMockServer>().isRunning }
//    }
//
//    @Test
//    fun `Starting mock server should fail when wire mock does not start`() {
//        every { anyConstructed<WireMockServer>().start() } just runs
//        every { anyConstructed<WireMockServer>().isRunning } returns false
//
//        val server = MockServer(name = "service", hostname = 9000, stubs = listOf())
//        assertFailsWith<>(ServiceNotHealthyException::class.java) { server.start() }
//
//        assertFalse(server.isRunning())
//        verify { anyConstructed<WireMockServer>().start() }
//        verify { anyConstructed<WireMockServer>().isRunning }
//    }
//
//    @Test
//    fun `Starting mock server should fail when wire mock fails`() {
//        every { anyConstructed<WireMockServer>().start() } throws FatalStartupException(RuntimeException())
//
//        val server = MockServer(name = "service", hostname = 9000, stubs = listOf())
//        assertFailsWith<>(ServiceStartupException::class.java) { server.start() }
//
//        verify { anyConstructed<WireMockServer>().start() }
//    }
//
//    @Test
//    fun `Stopping mock server should stop wire mock`() {
//        every { anyConstructed<WireMockServer>().stop() } just runs
//
//        val server = MockServer(name = "service", hostname = 9000, stubs = listOf())
//        assertDoesNotThrow { server.stop() }
//
//        verify { anyConstructed<WireMockServer>().stop() }
//    }
//
//    @Test
//    fun `Verifying that mock server received request should succeed`() {
//        val server = MockServer(name = "service", hostname = 9000, stubs = listOf())
//        server.verify(MockServer.receivedRequestMatching(HttpMethod.GET, path = StringMatcher(equals = "/search")).withBodyPatterns(listOf()))
//    }
//
//    private fun mockParsingStub(): MockServerStub {
//        val stub = mockk<MockServerStub>()
//        every { anyConstructed<YamlMockServerStubParser>().parseFile(any()) } returns stub
//        every { stub.registerAt(any()) } just runs
//
//        return stub
//    }
}
