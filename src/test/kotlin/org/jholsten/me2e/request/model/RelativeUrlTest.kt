package org.jholsten.me2e.request.model

import kotlin.test.*

internal class RelativeUrlTest {

    @Test
    fun `Building relative URL with path should succeed`() {
        val url = RelativeUrl.Builder()
            .withPath("/search")
            .build()

        assertEquals("/search", url.value)
    }

    @Test
    fun `Building relative URL with multiple parts should succeed`() {
        val url = RelativeUrl.Builder()
            .withPath("/search/some/value")
            .build()

        assertEquals("/search/some/value", url.value)
    }

    @Test
    fun `Building relative URL with path without leading slash should succeed`() {
        val url = RelativeUrl.Builder()
            .withPath("search")
            .build()

        assertEquals("/search", url.value)
    }

    @Test
    fun `Building relative URL with path only consisting of slash should succeed`() {
        val url = RelativeUrl.Builder()
            .withPath("/")
            .build()

        assertEquals("", url.value)
    }

    @Test
    fun `Building relative URL with path with multiple leading slashes should succeed`() {
        val url = RelativeUrl.Builder()
            .withPath("//search")
            .build()

        assertEquals("/search", url.value)
    }

    @Test
    fun `Building relative URL with empty path should succeed`() {
        val url = RelativeUrl.Builder()
            .withPath("")
            .build()

        assertEquals("", url.value)
    }

    @Test
    fun `Building relative URL with query parameter should succeed`() {
        val url = RelativeUrl.Builder()
            .withQueryParameter("key", "value")
            .build()

        assertEquals("?key=value", url.value)
    }

    @Test
    fun `Building relative URL with multiple query parameters should succeed`() {
        val url = RelativeUrl.Builder()
            .withQueryParameter("query", "value1")
            .withQueryParameter("query", "value2")
            .build()

        assertEquals("?query=value1&query=value2", url.value)
    }

    @Test
    fun `Building relative URL with multiple query parameters as list should succeed`() {
        val url = RelativeUrl.Builder()
            .withQueryParameter("query", listOf("value1", "value2"))
            .build()

        assertEquals("?query=value1&query=value2", url.value)
    }

    @Test
    fun `Building relative URL with fragment should succeed`() {
        val url = RelativeUrl.Builder()
            .withFragment("p=42")
            .build()

        assertEquals("#p=42", url.value)
    }

    @Test
    fun `Building relative URL with all optional values should succeed`() {
        val url = RelativeUrl.Builder()
            .withPath("/search")
            .withQueryParameter("query", "value1")
            .withQueryParameter("query", "value2")
            .withQueryParameter("param", "other-value")
            .withFragment("page=42")
            .build()

        assertEquals("/search?query=value1&query=value2&param=other-value#page=42", url.value)
    }

    @Test
    fun `Building empty relative URL should succeed`() {
        assertEquals("", RelativeUrl.empty().value)
    }
}
