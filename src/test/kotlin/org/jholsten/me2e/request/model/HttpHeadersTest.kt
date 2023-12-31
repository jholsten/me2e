package org.jholsten.me2e.request.model

import org.jholsten.util.RecursiveComparison
import kotlin.test.*

internal class HttpHeadersTest {

    @Test
    fun `Building HTTP headers should succeed`() {
        val headers = HttpHeaders.Builder()
            .add("key1", "value")
            .add("key2", "value1")
            .add("key2", "value2")
            .build()

        assertEquals(3, headers.size)
        assertHeadersAsExpected(
            expected = mapOf("key1" to listOf("value"), "key2" to listOf("value1", "value2")),
            headers = headers,
        )
    }

    @Test
    fun `Building HTTP headers with list should succeed`() {
        val headers = HttpHeaders.Builder()
            .add("key1", listOf("value"))
            .add("key2", listOf("value1", "value2"))
            .build()

        assertEquals(3, headers.size)
        assertHeadersAsExpected(
            expected = mapOf("key1" to listOf("value"), "key2" to listOf("value1", "value2")),
            headers = headers,
        )
    }

    @Test
    fun `Building HTTP headers adding multiple lists should succeed`() {
        val headers = HttpHeaders.Builder()
            .add("key", listOf("value1", "value2"))
            .add("key", listOf("value3", "value4"))
            .build()

        assertEquals(4, headers.size)
        assertHeadersAsExpected(
            expected = mapOf("key" to listOf("value1", "value2", "value3", "value4")),
            headers = headers,
        )
    }

    @Test
    fun `Building HTTP headers with empty key should fail`() {
        val e = assertFailsWith<IllegalArgumentException> { HttpHeaders.Builder().add("", "value") }
        assertEquals("Key cannot be blank", e.message)
    }

    @Test
    fun `Building HTTP headers with empty key for adding list should fail`() {
        val e = assertFailsWith<IllegalArgumentException> { HttpHeaders.Builder().add("", listOf("value")) }
        assertEquals("Key cannot be blank", e.message)
    }

    @Test
    fun `Building HTTP headers with empty value list should fail`() {
        val e = assertFailsWith<IllegalArgumentException> { HttpHeaders.Builder().add("key", listOf()) }
        assertEquals("List of values need to contain at least one entry", e.message)
    }

    @Test
    fun `Removing existing key-value-pair should succeed`() {
        val builder = HttpHeaders.Builder().add("key", "value").add("another-key", "another-value")
        val original = builder.build()
        val modified = builder.remove("key", "value").build()

        assertEquals(2, original.size)
        assertEquals(1, modified.size)
        assertHeadersAsExpected(mapOf("key" to listOf("value"), "another-key" to listOf("another-value")), original)
        assertHeadersAsExpected(mapOf("another-key" to listOf("another-value")), modified)
    }

    @Test
    fun `Removing existing key-value-pair from list should succeed`() {
        val builder = HttpHeaders.Builder().add("key", listOf("value1", "value2")).add("another-key", "another-value")
        val original = builder.build()
        val modified = builder.remove("key", "value1").build()

        assertEquals(3, original.size)
        assertEquals(2, modified.size)
        assertHeadersAsExpected(mapOf("key" to listOf("value1", "value2"), "another-key" to listOf("another-value")), original)
        assertHeadersAsExpected(mapOf("key" to listOf("value2"), "another-key" to listOf("another-value")), modified)
    }

    @Test
    fun `Removing non-existing key in key-value-pair should succeed`() {
        val builder = HttpHeaders.Builder().add("key", "value").add("another-key", "another-value")
        val original = builder.build()
        val modified = builder.remove("non-existing", "value").build()

        assertEquals(2, original.size)
        assertEquals(2, modified.size)
        assertHeadersAsExpected(mapOf("key" to listOf("value"), "another-key" to listOf("another-value")), original)
        assertHeadersAsExpected(mapOf("key" to listOf("value"), "another-key" to listOf("another-value")), modified)
    }

    @Test
    fun `Removing non-existing value in key-value-pair should succeed`() {
        val builder = HttpHeaders.Builder().add("key", "value").add("another-key", "another-value")
        val original = builder.build()
        val modified = builder.remove("key", "non-existing").build()

        assertEquals(2, original.size)
        assertEquals(2, modified.size)
        assertHeadersAsExpected(mapOf("key" to listOf("value"), "another-key" to listOf("another-value")), original)
        assertHeadersAsExpected(mapOf("key" to listOf("value"), "another-key" to listOf("another-value")), modified)
    }

    @Test
    fun `Removing existing key should succeed`() {
        val builder = HttpHeaders.Builder().add("key", "value").add("another-key", "another-value")
        val original = builder.build()
        val modified = builder.remove("key").build()

        assertEquals(2, original.size)
        assertEquals(1, modified.size)
        assertHeadersAsExpected(mapOf("key" to listOf("value"), "another-key" to listOf("another-value")), original)
        assertHeadersAsExpected(mapOf("another-key" to listOf("another-value")), modified)
    }

    @Test
    fun `Removing non-existing key should succeed`() {
        val builder = HttpHeaders.Builder().add("key", "value").add("another-key", "another-value")
        val original = builder.build()
        val modified = builder.remove("non-existing").build()

        assertEquals(2, original.size)
        assertEquals(2, modified.size)
        assertHeadersAsExpected(mapOf("key" to listOf("value"), "another-key" to listOf("another-value")), original)
        assertHeadersAsExpected(mapOf("key" to listOf("value"), "another-key" to listOf("another-value")), modified)
    }

    @Test
    fun `Setting value of existing key should replace all values`() {
        val builder = HttpHeaders.Builder().add("key", listOf("value1", "value2")).add("another-key", "another-value")
        builder["key"] = "other"
        val headers = builder.build()

        assertEquals(2, headers.size)
        assertHeadersAsExpected(mapOf("key" to listOf("other"), "another-key" to listOf("another-value")), headers)
    }

    @Test
    fun `Setting value of non-existing key should add entry`() {
        val builder = HttpHeaders.Builder().add("key", "value").add("another-key", "another-value")
        builder["other"] = "other-value"
        val headers = builder.build()

        assertEquals(3, headers.size)
        assertHeadersAsExpected(
            expected = mapOf("key" to listOf("value"), "another-key" to listOf("another-value"), "other" to listOf("other-value")),
            headers = headers
        )
    }

    @Test
    fun `Retrieving values for existing key should return values`() {
        val headers = HttpHeaders.Builder().add("key", "value").build()
        assertNotNull(headers["key"])
        assertEquals(listOf("value"), headers["key"])
        assertNotNull(headers.get("key"))
        assertEquals(listOf("value"), headers.get("key"))
    }

    @Test
    fun `Retrieving values for non-existing key should return null`() {
        val headers = HttpHeaders.Builder().add("key", "value").build()
        assertNull(headers["other-key"])
        assertNull(headers.get("other-key"))
    }

    @Test
    fun `Checking if headers contain existing key should return true`() {
        val headers = HttpHeaders.Builder().add("key", "value").build()
        assertTrue("key" in headers)
        assertTrue(headers.contains("key"))
    }

    @Test
    fun `Checking if headers contain non-existing key should return false`() {
        val headers = HttpHeaders.Builder().add("key", "value").build()
        assertFalse("other-key" in headers)
        assertFalse(headers.contains("other-key"))
    }

    @Test
    fun `Iterator should iterate over all entries`() {
        val headers = HttpHeaders.Builder()
            .add("key", "value")
            .add("key2", listOf("value1", "value2"))
            .build()

        val expected = mapOf("key" to listOf("value"), "key2" to listOf("value1", "value2"))
        val actual = mutableMapOf<String, List<String>>()
        for ((key, values) in headers) {
            actual[key] = values
        }
        RecursiveComparison.assertEquals(expected, actual)
        RecursiveComparison.assertEquals(expected, headers.entries)
    }

    @Test
    fun `Instantiating new builder should set all values`() {
        val original = HttpHeaders.Builder().add("key1", "value1").add("key2", "value2").build()
        val new = original.newBuilder().build()

        assertNotSame(original, new)
        assertNotSame(original.entries, new.entries)
        RecursiveComparison.assertEquals(original.entries, new.entries)
    }

    @Test
    fun `Modifying new builder should not modify original`() {
        val original = HttpHeaders.Builder().add("key1", "value1").add("key2", "value2").build()
        val new = original.newBuilder().add("key3", "value3").build()

        assertEquals(2, original.size)
        assertEquals(3, new.size)
        assertHeadersAsExpected(
            expected = mapOf("key1" to listOf("value1"), "key2" to listOf("value2")),
            headers = original,
        )
        assertHeadersAsExpected(
            expected = mapOf("key1" to listOf("value1"), "key2" to listOf("value2"), "key3" to listOf("value3")),
            headers = new,
        )
    }

    @Test
    fun `Instantiating empty headers should not have any entries`() {
        val headers = HttpHeaders.empty()

        assertEquals(0, headers.size)
        assertHeadersAsExpected(mapOf(), headers)
    }

    private fun assertHeadersAsExpected(expected: Map<String, List<String>>, headers: HttpHeaders) {
        val headersMap = headers.entries
        assertEquals(expected.size, headersMap.size)
        for ((key, values) in expected) {
            assertTrue(key in headers)
            RecursiveComparison.assertEquals(values, headers[key])
        }
    }
}
