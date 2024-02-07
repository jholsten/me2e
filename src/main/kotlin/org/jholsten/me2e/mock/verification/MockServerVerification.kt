package org.jholsten.me2e.mock.verification

import org.jholsten.me2e.mock.MockServer

/**
 * Model for asserting that a Mock Server instance received an expected request.
 *
 * Example Usage:
 * ```kotlin
 * import org.jholsten.me2e.assertions.*
 * import org.jholsten.me2e.Me2eTest
 * import org.jholsten.me2e.container.injection.InjectService
 * import org.jholsten.me2e.container.microservice.MicroserviceContainer
 * import org.jholsten.me2e.mock.MockServer
 *
 * class E2ETest : Me2eTest() {
 *     @InjectService
 *     private lateinit var mockServer: MockServer
 *
 *     @InjectService
 *     private lateinit var api: MicroserviceContainer
 *
 *     @Test
 *     fun `Invoking endpoint should invoke Mock Server`() {
 *         val url = RelativeUrl.Builder().withPath("/books").withQueryParameter("id", "1234").build()
 *         api.get(url)
 *
 *         assertThat(mockServer).receivedRequest(1, ExpectedRequest()
 *              .withPath(equalTo("/books"))
 *              .withQueryParameters(containsKey("id").withValue(equalTo("1234")))
 *              .andNoOther()
 *         )
 *     }
 * }
 * ```
 * @see org.jholsten.me2e.assertions.assertThat
 */
class MockServerVerification internal constructor(private val mockServer: MockServer) {

    /**
     * Entrypoint for specifying a request that a Mock Server should have received at least once.
     * @param expected Expectation for the request that the Mock Server should have received.
     */
    fun receivedRequest(expected: ExpectedRequest) {
        mockServer.verify(null, expected)
    }

    /**
     * Entrypoint for specifying a request that a Mock Server should have received exactly [times] number of times.
     * @param times Number of times that the Mock Server should have received this request.
     * @param expected Expectation for the request that the Mock Server should have received.
     */
    fun receivedRequest(times: Int, expected: ExpectedRequest) {
        mockServer.verify(times, expected)
    }
}
