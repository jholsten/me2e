package org.jholsten.me2e.request.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class HttpMethodTest {

    @ParameterizedTest(name = "[{index}] {0} should require a request body")
    @EnumSource(HttpMethod::class, names = ["POST", "PUT", "PATCH"])
    fun `HTTP methods should require a request body`(method: HttpMethod) {
        assertTrue(method.requiresRequestBody())
    }

    @ParameterizedTest(name = "[{index}] {0} should not require a request body")
    @EnumSource(HttpMethod::class, names = ["POST", "PUT", "PATCH"], mode = EnumSource.Mode.EXCLUDE)
    fun `HTTP methods should not require a request body`(method: HttpMethod) {
        assertFalse(method.requiresRequestBody())
    }

    @ParameterizedTest(name = "[{index}] {0} should allow a request body")
    @EnumSource(HttpMethod::class, names = ["GET", "HEAD"])
    fun `HTTP methods should allow a request body`(method: HttpMethod) {
        assertFalse(method.allowsRequestBody())
    }

    @ParameterizedTest(name = "[{index}] {0} should not allow a request body")
    @EnumSource(HttpMethod::class, names = ["GET", "HEAD"], mode = EnumSource.Mode.EXCLUDE)
    fun `HTTP methods should not allow a request body`(method: HttpMethod) {
        assertTrue(method.allowsRequestBody())
    }
}
