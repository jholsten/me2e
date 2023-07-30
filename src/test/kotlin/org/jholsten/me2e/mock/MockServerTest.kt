package org.jholsten.me2e.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.mockk.*
import org.jholsten.me2e.mock.parser.YamlMockServerStubParser
import org.jholsten.me2e.mock.stubbing.MockServerStub
import org.jholsten.me2e.request.mapper.HttpRequestMapper
import org.jholsten.util.RecursiveComparison
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


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

    private fun mockParsingStub(): MockServerStub {
        val stub = mockk<MockServerStub>()
        every { anyConstructed<YamlMockServerStubParser>().parseFile(any()) } returns stub
        every { stub.register(any()) } just runs

        return stub
    }
}
