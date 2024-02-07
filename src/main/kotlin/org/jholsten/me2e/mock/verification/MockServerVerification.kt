package org.jholsten.me2e.mock.verification

import org.jholsten.me2e.mock.MockServer

/**
 * Model for asserting that a Mock Server instance received an expected request.
 * @see org.jholsten.me2e.assertions.assertThat
 */
class MockServerVerification internal constructor(private val mockServer: MockServer) {

    /**
     * Number of times that the Mock Server should have received this request.
     * If set to `null`, it is verified that the Mock Server received the specified request at least once.
     */
    private var times: Int? = null

    /**
     * Entrypoint for specifying a request that a Mock Server should have received at least once.
     */
    fun receivedRequest(expected: ExpectedRequest) = apply {
        mockServer.verify(null, expected)
    }

    /**
     * Entrypoint for specifying a request that a Mock Server should have received exactly [times] number of times.
     * @param times Number of times that the Mock Server should have received this request.
     */
    fun receivedRequest(times: Int, expected: ExpectedRequest) = apply {
        this.times = times
        mockServer.verify(times, expected)
    }
}
