package org.jholsten.me2e.request.model

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame

internal class UrlTest {
    @Test
    fun `Building minimal URL should succeed`() {
        val url = Url.Builder()
            .withScheme(Url.Scheme.HTTP)
            .withHost("example.com")
            .build()

        assertEquals("http://example.com", url.value)
    }

    @Test
    fun `Building URL with host with trailing slashes should succeed`() {
        val url = Url.Builder()
            .withScheme(Url.Scheme.HTTP)
            .withHost("example.com//")
            .withPath("/search")
            .build()

        assertEquals("http://example.com/search", url.value)
    }

    @Test
    fun `Building URL with port should succeed`() {
        val url = Url.Builder()
            .withScheme(Url.Scheme.HTTP)
            .withHost("example.com")
            .withPort(8080)
            .build()

        assertEquals("http://example.com:8080", url.value)
    }

    @Test
    fun `Building URL with path should succeed`() {
        val url = Url.Builder()
            .withScheme(Url.Scheme.HTTP)
            .withHost("example.com")
            .withPath("/search")
            .build()

        assertEquals("http://example.com/search", url.value)
    }

    @Test
    fun `Building URL with path with multiple parts should succeed`() {
        val url = Url.Builder()
            .withScheme(Url.Scheme.HTTP)
            .withHost("example.com")
            .withPath("/search/some/value")
            .build()

        assertEquals("http://example.com/search/some/value", url.value)
    }

    @Test
    fun `Building URL with path without leading slash should succeed`() {
        val url = Url.Builder()
            .withScheme(Url.Scheme.HTTP)
            .withHost("example.com")
            .withPath("search")
            .build()

        assertEquals("http://example.com/search", url.value)
    }

    @Test
    fun `Building URL with path only consisting of slash should succeed`() {
        val url = Url.Builder()
            .withScheme(Url.Scheme.HTTP)
            .withHost("example.com")
            .withPath("/")
            .build()

        assertEquals("http://example.com", url.value)
    }

    @Test
    fun `Building URL with path with multiple leading slashes should succeed`() {
        val url = Url.Builder()
            .withScheme(Url.Scheme.HTTP)
            .withHost("example.com")
            .withPath("//search")
            .build()

        assertEquals("http://example.com/search", url.value)
    }

    @Test
    fun `Building URL with query parameter should succeed`() {
        val url = Url.Builder()
            .withScheme(Url.Scheme.HTTP)
            .withHost("example.com")
            .withQueryParameter("key", "value")
            .build()

        assertEquals("http://example.com?key=value", url.value)
    }

    @Test
    fun `Building URL with multiple query parameters should succeed`() {
        val url = Url.Builder()
            .withScheme(Url.Scheme.HTTP)
            .withHost("example.com")
            .withQueryParameter("query", "value1")
            .withQueryParameter("query", "value2")
            .build()

        assertEquals("http://example.com?query=value1&query=value2", url.value)
    }

    @Test
    fun `Building URL with multiple query parameters as list should succeed`() {
        val url = Url.Builder()
            .withScheme(Url.Scheme.HTTP)
            .withHost("example.com")
            .withQueryParameter("query", listOf("value1", "value2"))
            .build()

        assertEquals("http://example.com?query=value1&query=value2", url.value)
    }

    @Test
    fun `Building URL with fragment should succeed`() {
        val url = Url.Builder()
            .withScheme(Url.Scheme.HTTP)
            .withHost("example.com")
            .withFragment("page=42")
            .build()

        assertEquals("http://example.com#page=42", url.value)
    }

    @Test
    fun `Building URL with all optional values should succeed`() {
        val url = Url.Builder()
            .withScheme(Url.Scheme.HTTP)
            .withHost("example.com")
            .withPort(8080)
            .withPath("/search")
            .withQueryParameter("query", "value1")
            .withQueryParameter("query", "value2")
            .withQueryParameter("param", "other-value")
            .withFragment("page=42")
            .build()

        assertEquals("http://example.com:8080/search?query=value1&query=value2&param=other-value#page=42", url.value)
    }

    @Test
    fun `Building URL without scheme should fail`() {
        assertFailsWith<IllegalArgumentException> {
            Url.Builder().withHost("example.com").build()
        }
    }

    @Test
    fun `Building URL without host should fail`() {
        assertFailsWith<IllegalArgumentException> {
            Url.Builder().withScheme(Url.Scheme.HTTPS).build()
        }
    }

    @Test
    fun `Building URL with invalid port number should fail`() {
        assertFailsWith<IllegalArgumentException> {
            Url.Builder().withPort(-1).build()
        }
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "https://example.com",
            "https://example.com:8080",
            "https://example.com/search",
            "https://example.com?query=value1&query=value2",
            "https://example.com#page=42",
            "http://example.com:8080/search?query=value1&query=value2&param=other-value#page=42",
        ]
    )
    fun `Building new builder for URL should result in the original URL`(urlValue: String) {
        val url = Url(urlValue)
        val result = url.newBuilder().build()

        assertEquals(url.value, result.value)
        assertNotSame(url, result)
    }

    @Test
    fun `Modifying URL builder should succeed`() {
        val url = Url("https://example.com:8080/search?query=value1&param=other-value#page=42")
        val result = url.newBuilder()
            .withScheme(Url.Scheme.HTTP)
            .withHost("google.com")
            .withPort(90)
            .withPath("/other")
            .withQueryParameter("query", "value2")
            .withQueryParameter("new", "val")
            .withFragment("number=12")
            .build()

        assertEquals("http://google.com:90/other?query=value1&query=value2&param=other-value&new=val#number=12", result.value)
    }

    @Test
    fun `Generating builder for invalid URL should fail`() {
        assertFailsWith<IllegalArgumentException> { Url("invalid").newBuilder() }
    }

    @ParameterizedTest(name = "[{index}] Value \"{0}\"")
    @ValueSource(strings = ["invalid", "", "https://", "/search"])
    fun `Instantiating invalid URL should fail`(value: String) {
        val e = assertFailsWith<IllegalArgumentException> { Url(value) }
        assertEquals("Invalid URL format", e.message)
    }

    @ParameterizedTest(name = "[{index}] Relative URL \"{0}\"")
    @CsvSource(
        "/search, https://example.com/search",
        "search, https://example.com/search",
        "//search, https://example.com/search",
        "/search?q=1&q=2#p=42, https://example.com/search?q=1&q=2#p=42",
        "?q=1, https://example.com?q=1",
        "#p=42, https://example.com#p=42"
    )
    fun `Appending relative URL should succeed`(relativeUrl: String, expectedUrl: String) {
        assertEquals(expectedUrl, Url("https://example.com").withRelativeUrl(RelativeUrl(relativeUrl)).value)
    }
}
