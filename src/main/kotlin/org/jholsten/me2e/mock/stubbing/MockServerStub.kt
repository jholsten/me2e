package org.jholsten.me2e.mock.stubbing

import org.jholsten.me2e.mock.stubbing.request.MockServerStubRequest

/**
 * Stub defining how the Mock Server should respond on certain requests.
 */
class MockServerStub(
    /**
     * Request to which the stub should respond.
     */
    val request: MockServerStubRequest,
)
