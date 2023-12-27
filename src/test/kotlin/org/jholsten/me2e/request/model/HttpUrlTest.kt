package org.jholsten.me2e.request.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class HttpUrlTest {
    @Test
    fun `Building minimal URL should succeed`() {
        val httpUrl = HttpUrl.Builder()
            .withScheme(HttpUrl.Scheme.HTTP)
            .withHost("example.com")
            .build()

        assertEquals("http://example.com", httpUrl.value)
    }

    @Test
    fun `Building URL with port should succeed`() {
        val httpUrl = HttpUrl.Builder()
            .withScheme(HttpUrl.Scheme.HTTP)
            .withHost("example.com")
            .withPort(8080)
            .build()

        assertEquals("http://example.com:8080", httpUrl.value)
    }

    @Test
    fun `Building URL with path should succeed`() {
        val httpUrl = HttpUrl.Builder()
            .withScheme(HttpUrl.Scheme.HTTP)
            .withHost("example.com")
            .withPath("/search")
            .build()

        assertEquals("http://example.com/search", httpUrl.value)
    }

    @Test
    fun `Building URL with path with multiple parts should succeed`() {
        val httpUrl = HttpUrl.Builder()
            .withScheme(HttpUrl.Scheme.HTTP)
            .withHost("example.com")
            .withPath("/search/some/value")
            .build()

        assertEquals("http://example.com/search/some/value", httpUrl.value)
    }

    @Test
    fun `Building URL with path without leading slash should succeed`() {
        val httpUrl = HttpUrl.Builder()
            .withScheme(HttpUrl.Scheme.HTTP)
            .withHost("example.com")
            .withPath("search")
            .build()

        assertEquals("http://example.com/search", httpUrl.value)
    }

    @Test
    fun `Building URL with path only consisting of slash should succeed`() {
        val httpUrl = HttpUrl.Builder()
            .withScheme(HttpUrl.Scheme.HTTP)
            .withHost("example.com")
            .withPath("/")
            .build()

        assertEquals("http://example.com", httpUrl.value)
    }

    @Test
    fun `Building URL with query parameter should succeed`() {
        val httpUrl = HttpUrl.Builder()
            .withScheme(HttpUrl.Scheme.HTTP)
            .withHost("example.com")
            .withQueryParameter("key", "value")
            .build()

        assertEquals("http://example.com?key=value", httpUrl.value)
    }

    @Test
    fun `Building URL with multiple query parameters should succeed`() {
        val httpUrl = HttpUrl.Builder()
            .withScheme(HttpUrl.Scheme.HTTP)
            .withHost("example.com")
            .withQueryParameter("query", "value1")
            .withQueryParameter("query", "value2")
            .build()

        assertEquals("http://example.com?query=value1&query=value2", httpUrl.value)
    }

    @Test
    fun `Building URL with multiple query parameters as list should succeed`() {
        val httpUrl = HttpUrl.Builder()
            .withScheme(HttpUrl.Scheme.HTTP)
            .withHost("example.com")
            .withQueryParameter("query", listOf("value1", "value2"))
            .build()

        assertEquals("http://example.com?query=value1&query=value2", httpUrl.value)
    }

    @Test
    fun `Building URL with fragment should succeed`() {
        val httpUrl = HttpUrl.Builder()
            .withScheme(HttpUrl.Scheme.HTTP)
            .withHost("example.com")
            .withFragment("page=42")
            .build()

        assertEquals("http://example.com#page=42", httpUrl.value)
    }

    @Test
    fun `Building URL with all optional values should succeed`() {
        val httpUrl = HttpUrl.Builder()
            .withScheme(HttpUrl.Scheme.HTTP)
            .withHost("example.com")
            .withPort(8080)
            .withPath("/search")
            .withQueryParameter("query", "value1")
            .withQueryParameter("query", "value2")
            .withQueryParameter("param", "other-value")
            .withFragment("page=42")
            .build()

        assertEquals("http://example.com:8080/search?query=value1&query=value2&param=other-value#page=42", httpUrl.value)
    }

    @Test
    fun `Building URL without scheme should fail`() {
        assertFailsWith<IllegalArgumentException> {
            HttpUrl.Builder().withHost("example.com").build()
        }
    }

    @Test
    fun `Building URL without host should fail`() {
        assertFailsWith<IllegalArgumentException> {
            HttpUrl.Builder().withScheme(HttpUrl.Scheme.HTTPS).build()
        }
    }

    @Test
    fun `Building URL with invalid port number should fail`() {
        assertFailsWith<IllegalArgumentException> {
            HttpUrl.Builder().withPort(-1).build()
        }
    }
}
