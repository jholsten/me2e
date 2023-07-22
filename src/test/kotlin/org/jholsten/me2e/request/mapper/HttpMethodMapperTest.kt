package org.jholsten.me2e.request.mapper

import org.jholsten.me2e.request.model.HttpMethod
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class HttpMethodMapperTest {

    @Test
    fun `GET should be mapped to GET`() {
        assertEquals(HttpMethod.GET, HttpMethodMapper.INSTANCE.toInternalDto("GET"))
    }

    @Test
    fun `PUT should be mapped to PUT`() {
        assertEquals(HttpMethod.PUT, HttpMethodMapper.INSTANCE.toInternalDto("PUT"))
    }

    @Test
    fun `POST should be mapped to POST`() {
        assertEquals(HttpMethod.POST, HttpMethodMapper.INSTANCE.toInternalDto("POST"))
    }

    @Test
    fun `PATCH should be mapped to PATCH`() {
        assertEquals(HttpMethod.PATCH, HttpMethodMapper.INSTANCE.toInternalDto("PATCH"))
    }

    @Test
    fun `DELETE should be mapped to DELETE`() {
        assertEquals(HttpMethod.DELETE, HttpMethodMapper.INSTANCE.toInternalDto("DELETE"))
    }

    @Test
    fun `HEAD should be mapped to HEAD`() {
        assertEquals(HttpMethod.HEAD, HttpMethodMapper.INSTANCE.toInternalDto("HEAD"))
    }

    @Test
    fun `OPTIONS should be mapped to OPTIONS`() {
        assertEquals(HttpMethod.OPTIONS, HttpMethodMapper.INSTANCE.toInternalDto("OPTIONS"))
    }

    @Test
    fun `UNKNOWN should be mapped to UNKNOWN`() {
        assertEquals(HttpMethod.UNKNOWN, HttpMethodMapper.INSTANCE.toInternalDto("UNKNOWN"))
    }

    @Test
    fun `Anything else should be mapped to UNKNOWN`() {
        assertEquals(HttpMethod.UNKNOWN, HttpMethodMapper.INSTANCE.toInternalDto("anything"))
    }
}
