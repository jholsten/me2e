package org.jholsten.me2e.mock

import com.fasterxml.jackson.core.type.TypeReference
import com.github.tomakehurst.wiremock.http.JvmProxyConfigurer
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.apache.hc.core5.http.ContentType as ApacheContentType
import org.apache.hc.core5.http.HttpEntity
import org.apache.hc.core5.http.io.entity.StringEntity
import org.jholsten.me2e.assertions.*
import org.jholsten.me2e.config.model.MockServerConfig
import org.jholsten.me2e.mock.verification.exception.VerificationException
import org.jholsten.me2e.mock.stubbing.MockServerStub
import org.jholsten.me2e.mock.stubbing.request.MockServerStubRequestMatcher
import org.jholsten.me2e.mock.stubbing.request.StringMatcher
import org.jholsten.me2e.mock.stubbing.response.MockServerStubResponse
import org.jholsten.me2e.mock.stubbing.response.MockServerStubResponseBody
import org.jholsten.me2e.mock.verification.ExpectedRequest
import org.jholsten.me2e.request.assertions.AssertableResponseIT
import org.jholsten.me2e.request.model.*
import org.jholsten.util.RecursiveComparison
import org.jholsten.util.assertDoesNotThrow
import kotlin.test.*

class MockServerIT {

    private val client = HttpClientBuilder.create()
        .useSystemProperties()
        .build()

    private val exampleServer = MockServer(
        "example-service", "example.com", listOf(
            MockServerStub(
                name = "other-stub",
                request = MockServerStubRequestMatcher(
                    hostname = "example.com",
                    method = HttpMethod.POST,
                    path = StringMatcher(equals = "/search"),
                    queryParameters = mapOf("id" to StringMatcher(equals = "123"))
                ),
                response = MockServerStubResponse(
                    statusCode = 200,
                    body = MockServerStubResponseBody(
                        stringContent = "A Response",
                    ),
                    headers = mapOf("Content-Type" to listOf("text/plain")),
                )
            ),
            MockServerStub(
                name = "request-stub",
                request = MockServerStubRequestMatcher(
                    hostname = "example.com",
                    method = HttpMethod.GET,
                    path = StringMatcher(equals = "/"),
                ),
                response = MockServerStubResponse(
                    statusCode = 200,
                    body = MockServerStubResponseBody(
                        stringContent = "Some Response"
                    ),
                    headers = mapOf("Content-Type" to listOf("text/plain"))
                )
            )
        )
    )

    private val expectedObj = AssertableResponseIT.BodyClass(
        name = "John",
        nested = AssertableResponseIT.NestedBodyClass(
            key1 = "value1",
            key2 = "value2",
        ),
        details = listOf(
            AssertableResponseIT.DetailsBodyClass(detail = 1),
            AssertableResponseIT.DetailsBodyClass(detail = 2),
        )
    )

    private val filename = "responses/expected_body.json"

    private val mockServerConfig = MockServerConfig()

    private val manager = MockServerManager(mapOf("example-service" to exampleServer), mockServerConfig)

    @BeforeTest
    fun beforeTest() {
        manager.start()
        assertTrue(manager.isRunning)
        assertTrue(exampleServer.isRunning)
        JvmProxyConfigurer.configureFor(manager.httpPort)
    }

    @AfterTest
    fun afterTest() {
        manager.stop()
        assertFalse(manager.isRunning)
        assertFalse(exampleServer.isRunning)
        JvmProxyConfigurer.restorePrevious()
    }

    @Test
    fun `Mock Server should respond with stubbed response`() {
        val expectedReceivedRequest = HttpRequest(
            url = Url("http://example.com/search?id=123"),
            method = HttpMethod.POST,
            body = HttpRequestBody(
                byteArrayOf(
                    123, 34, 110, 97, 109, 101, 34, 58, 32, 34, 74, 111, 104, 110, 34, 44, 32, 34, 110, 101, 115, 116, 101, 100, 34, 58,
                    32, 123, 34, 107, 101, 121, 49, 34, 58, 32, 34, 118, 97, 108, 117, 101, 49, 34, 44, 32, 34, 107, 101, 121, 50, 34, 58,
                    32, 34, 118, 97, 108, 117, 101, 50, 34, 125, 44, 32, 34, 100, 101, 116, 97, 105, 108, 115, 34, 58, 32, 91, 123, 34, 100,
                    101, 116, 97, 105, 108, 34, 58, 32, 49, 125, 44, 32, 123, 34, 100, 101, 116, 97, 105, 108, 34, 58, 32, 50, 125, 93, 125,
                ),
                ContentType.JSON_UTF8,
            ),
        )

        val request = HttpPost("http://example.com/search?id=123")
        request.entity = StringEntity(
            "{\"name\": \"John\", \"nested\": {\"key1\": \"value1\", \"key2\": \"value2\"}, \"details\": [{\"detail\": 1}, {\"detail\": 2}]}"
        )
        val response = client.execute(request)

        assertEquals(200, response?.code)
        assertEquals("A Response", encodeResponseBody(response?.entity))
        assertEquals("text/plain", response?.getFirstHeader("Content-Type")?.value)
        assertEquals(1, exampleServer.requestsReceived.size)
        assertEquals(expectedReceivedRequest.url, exampleServer.requestsReceived.first().url)
        assertEquals(expectedReceivedRequest.method, exampleServer.requestsReceived.first().method)
        RecursiveComparison.assertEquals(expectedReceivedRequest.body?.asBinary(), exampleServer.requestsReceived.first().body?.asBinary())
        assertDoesNotThrow {
            assertThat(exampleServer).receivedRequest(
                1, ExpectedRequest()
                    .withPath(equalTo("/search"))
                    .withMethod(equalTo(HttpMethod.POST))
                    .withQueryParameters(containsKey("id").withValue(equalTo("123")))
                    .withBody(equalTo("{\"name\": \"John\", \"nested\": {\"key1\": \"value1\", \"key2\": \"value2\"}, \"details\": [{\"detail\": 1}, {\"detail\": 2}]}"))
                    .withBase64Body(equalTo("eyJuYW1lIjogIkpvaG4iLCAibmVzdGVkIjogeyJrZXkxIjogInZhbHVlMSIsICJrZXkyIjogInZhbHVlMiJ9LCAiZGV0YWlscyI6IFt7ImRldGFpbCI6IDF9LCB7ImRldGFpbCI6IDJ9XX0="))
                    .withJsonBody(containsNode("name").withValue(equalTo("John")))
                    .withJsonBody(containsNode("nested.key1").withValue(equalTo("value1")))
                    .withJsonBody(containsNode("nested.key2").withValue(equalTo("value2")))
                    .withJsonBody(containsNode("details[0].detail").withValue(equalTo("1")))
                    .withJsonBody(containsNode("details[1].detail").withValue(equalTo("2")))
                    .withObjectBody(AssertableResponseIT.BodyClass::class.java, equalTo(expectedObj))
                    .withObjectBody(object : TypeReference<AssertableResponseIT.BodyClass>() {}, equalTo(expectedObj))
                    .withObjectBody<AssertableResponseIT.BodyClass>(equalTo(expectedObj))
                    .withBody(equalToContentsFromFile(filename).asString())
                    .withBase64Body(equalToContentsFromFile(filename).asBase64())
                    .withJsonBody(equalToContentsFromFile(filename).asJson())
                    .withBinaryBody(equalToContentsFromFile(filename).asBinary())
                    .andNoOther()
            )
        }
    }

    @Test
    fun `Mock Server should respond with stubbed response for request with body`() {
        val expectedReceivedRequest = HttpRequest(
            url = Url("http://example.com/search?id=123"),
            method = HttpMethod.POST,
            body = HttpRequestBody("{\"some-key\": \"some-value\"}", ContentType.JSON_UTF8),
            headers = HttpHeaders(mapOf("header1" to listOf("headerValue"))),
        )

        val request = HttpPost("http://example.com/search?id=123")
        request.entity = StringEntity("{\"some-key\": \"some-value\"}", ApacheContentType.APPLICATION_JSON)
        request.setHeader("header1", "headerValue")
        val response = client.execute(request)

        assertEquals(200, response?.code)
        assertEquals("A Response", encodeResponseBody(response?.entity))
        assertEquals("text/plain", response?.getFirstHeader("Content-Type")?.value)
        assertEquals(1, exampleServer.requestsReceived.size)
        assertEquals(expectedReceivedRequest.url, exampleServer.requestsReceived.first().url)
        assertEquals(expectedReceivedRequest.method, exampleServer.requestsReceived.first().method)
        assertEquals(expectedReceivedRequest.body?.asString(), exampleServer.requestsReceived.first().body?.asString())
        assertDoesNotThrow {
            assertThat(exampleServer).receivedRequest(
                1, ExpectedRequest()
                    .withPath(equalTo("/search"))
                    .withMethod(equalTo(HttpMethod.POST))
                    .withQueryParameters(containsKey("id").withValue(equalTo("123")))
                    .withBody(equalTo("{\"some-key\": \"some-value\"}"))
                    .withJsonBody(containsNode("some-key").withValue(equalTo("some-value")))
                    .withHeaders(containsKey("header1").withValue(equalTo("headerValue")))
                    .withContentType(equalTo(ContentType.JSON_UTF8.value))
                    .andNoOther()
            )
        }
    }

    @Test
    fun `Mock Server verification should succeed for named stub`() {
        val response = client.execute(HttpGet("http://example.com"))

        assertEquals(200, response.code)
        assertEquals(1, exampleServer.requestsReceived.size)
        assertDoesNotThrow {
            assertThat(exampleServer).receivedRequest(1, ExpectedRequest().matchingStub("request-stub"))
        }
    }

    @Test
    fun `Mock Server verification should fail for named stub if request does not match`() {
        val response = client.execute(HttpGet("http://example.com"))

        assertEquals(200, response.code)
        assertEquals(1, exampleServer.requestsReceived.size)
        assertFailsWith<VerificationException> {
            assertThat(exampleServer).receivedRequest(1, ExpectedRequest().matchingStub("other-stub"))
        }
    }

    @Test
    fun `Mock Server verification should fail for non-existing named stub`() {
        val response = client.execute(HttpGet("http://example.com"))

        assertEquals(200, response.code)
        assertEquals(1, exampleServer.requestsReceived.size)
        assertFailsWith<IllegalArgumentException> {
            assertThat(exampleServer).receivedRequest(1, ExpectedRequest().matchingStub("non-existing"))
        }
    }

    @Test
    fun `Mock Server verification should fail if expected request was not received`() {
        val response = client.execute(HttpGet("http://example.com"))

        assertEquals(200, response.code)
        assertEquals(1, exampleServer.requestsReceived.size)
        assertDoesNotThrow {
            assertThat(exampleServer).receivedRequest(
                1, ExpectedRequest()
                    .withPath(equalTo("/"))
                    .withMethod(equalTo(HttpMethod.GET))
                    .andNoOther()
            )
        }
        assertFailsWith<VerificationException> {
            assertThat(exampleServer).receivedRequest(
                1, ExpectedRequest()
                    .withPath(equalTo("/something-else"))
                    .withMethod(equalTo(HttpMethod.GET))
                    .withContentType(equalTo(ContentType.TEXT_PLAIN_UTF8.value))
            )
        }
    }

    @Test
    fun `Mock Server verification should fail if expected request was not received at least once`() {
        assertEquals(0, exampleServer.requestsReceived.size)
        assertFailsWith<VerificationException> {
            assertThat(exampleServer).receivedRequest(
                ExpectedRequest()
                    .withPath(equalTo("/"))
                    .withMethod(equalTo(HttpMethod.GET))
            )
        }
    }

    @Test
    fun `Mock Server verification should fail if other requests were received`() {
        val response1 = client.execute(HttpGet("http://example.com"))
        val response2 = client.execute(HttpPost("http://example.com/search?id=123"))

        assertEquals(200, response1?.code)
        assertEquals(200, response2?.code)
        assertEquals(2, exampleServer.requestsReceived.size)
        val request1Verification = ExpectedRequest()
            .withPath(equalTo("/"))
            .withMethod(equalTo(HttpMethod.GET))
        val request2Verification = ExpectedRequest()
            .withPath(equalTo("/search"))
            .withMethod(equalTo(HttpMethod.POST))
            .withQueryParameters(containsKey("id").withValue(equalTo("123")))
        assertDoesNotThrow { assertThat(exampleServer).receivedRequest(request1Verification) }
        assertDoesNotThrow { assertThat(exampleServer).receivedRequest(request2Verification) }
        assertFailsWith<VerificationException> {
            assertThat(exampleServer).receivedRequest(request1Verification.andNoOther())
        }
        assertFailsWith<VerificationException> {
            assertThat(exampleServer).receivedRequest(request2Verification.andNoOther())
        }
    }

    @Test
    fun `Mock Server should respond with 404 if no stub mapping matches`() {
        val response = client.execute(HttpGet("http://example.com/not-stubbed"))

        assertEquals(404, response.code)
        assertEquals(1, exampleServer.requestsReceived.size)
        assertDoesNotThrow {
            assertThat(exampleServer).receivedRequest(
                1, ExpectedRequest()
                    .withPath(equalTo("/not-stubbed"))
                    .withMethod(equalTo(HttpMethod.GET))
                    .andNoOther()
            )
        }
    }

    @Test
    fun `Resetting Mock Server should reset stubs and requests`() {
        val response = client.execute(HttpGet("http://example.com"))

        assertEquals(200, response.code)
        assertEquals(1, exampleServer.requestsReceived.size)
        assertDoesNotThrow {
            assertThat(exampleServer).receivedRequest(
                1, ExpectedRequest()
                    .withPath(equalTo("/"))
                    .withMethod(equalTo(HttpMethod.GET))
            )
        }

        exampleServer.reset()

        assertEquals(0, exampleServer.requestsReceived.size)
        assertFailsWith<VerificationException> {
            assertThat(exampleServer).receivedRequest(
                1, ExpectedRequest()
                    .withPath(equalTo("/"))
                    .withMethod(equalTo(HttpMethod.GET))
            )
        }
    }

    private fun encodeResponseBody(body: HttpEntity?): String? {
        return body?.content?.let { String(it.readAllBytes()) }
    }
}
