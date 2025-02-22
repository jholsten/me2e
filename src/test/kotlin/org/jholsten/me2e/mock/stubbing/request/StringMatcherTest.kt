package org.jholsten.me2e.mock.stubbing.request

import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import kotlin.test.*

internal class StringMatcherTest {

    @Test
    fun `String matcher with equals should match`() {
        val matcher = StringMatcher(equals = "ABC", ignoreCase = false)

        assertTrue(matcher.matchesEqual("ABC"))
        assertFalse(matcher.matchesEqual("abc"))
        assertTrue(matcher.matches("ABC"))
        assertFalse(matcher.matches("abc"))
    }

    @Test
    fun `String matcher with equals should match ignoring case`() {
        val matcher = StringMatcher(equals = "ABC", ignoreCase = true)

        assertTrue(matcher.matchesEqual("ABC"))
        assertTrue(matcher.matchesEqual("abc"))
        assertTrue(matcher.matches("ABC"))
        assertTrue(matcher.matches("abc"))
    }

    @Test
    fun `String matcher with not equals should match`() {
        val matcher = StringMatcher(notEquals = "ABC")

        assertTrue(matcher.matchesEqual("abc"))
        assertTrue(matcher.matchesEqual("def"))
        assertTrue(matcher.matches("abc"))
        assertTrue(matcher.matches("def"))
    }

    @Test
    fun `String matcher with not equals should match ignoring case`() {
        val matcher = StringMatcher(notEquals = "ABC", ignoreCase = true)

        assertFalse(matcher.matchesEqual("abc"))
        assertTrue(matcher.matchesEqual("def"))
        assertFalse(matcher.matches("abc"))
        assertTrue(matcher.matches("def"))
    }

    @Test
    fun `String matcher with pattern should match`() {
        val matcher = StringMatcher(matches = "^[A-Z0-9._-]{7}$", ignoreCase = false)

        assertTrue(matcher.matchesPattern("A.1-2_3"))
        assertFalse(matcher.matchesPattern("a.1-2_3"))
        assertFalse(matcher.matchesPattern("nope"))
        assertTrue(matcher.matches("A.1-2_3"))
        assertFalse(matcher.matches("a.1-2_3"))
        assertFalse(matcher.matches("nope"))
    }

    @Test
    fun `String matcher with pattern should match ignoring case`() {
        val matcher = StringMatcher(matches = "^[A-Z0-9._-]{7}$", ignoreCase = true)

        assertTrue(matcher.matchesPattern("A.1-2_3"))
        assertTrue(matcher.matchesPattern("a.1-2_3"))
        assertFalse(matcher.matchesPattern("nope"))
        assertTrue(matcher.matches("A.1-2_3"))
        assertTrue(matcher.matches("a.1-2_3"))
        assertFalse(matcher.matches("nope"))
    }

    @Test
    fun `String matcher with pattern should not match`() {
        val matcher = StringMatcher(notMatches = "^[A-Z]{3}$", ignoreCase = false)

        assertTrue(matcher.matchesPattern("abc"))
        assertFalse(matcher.matchesPattern("ABC"))
        assertTrue(matcher.matchesPattern("123"))
        assertTrue(matcher.matches("abc"))
        assertFalse(matcher.matches("ABC"))
        assertTrue(matcher.matches("123"))
    }

    @Test
    fun `String matcher with pattern should not match ignoring case`() {
        val matcher = StringMatcher(notMatches = "^[A-Z]{3}$", ignoreCase = true)

        assertFalse(matcher.matchesPattern("abc"))
        assertFalse(matcher.matchesPattern("ABC"))
        assertTrue(matcher.matchesPattern("123"))
        assertFalse(matcher.matches("abc"))
        assertFalse(matcher.matches("ABC"))
        assertTrue(matcher.matches("123"))
    }

    @Test
    fun `String matcher with contains should match`() {
        val matcher = StringMatcher(contains = "A", ignoreCase = false)

        assertTrue(matcher.matchesContains("ABC"))
        assertFalse(matcher.matchesContains("abc"))
        assertTrue(matcher.matches("ABC"))
        assertFalse(matcher.matches("abc"))
    }

    @Test
    fun `String matcher with contains should match ignoring case`() {
        val matcher = StringMatcher(contains = "A", ignoreCase = true)

        assertTrue(matcher.matchesContains("ABC"))
        assertTrue(matcher.matchesContains("abc"))
        assertTrue(matcher.matches("ABC"))
        assertTrue(matcher.matches("abc"))
    }

    @Test
    fun `String matcher with contains should not match`() {
        val matcher = StringMatcher(notContains = "Z", ignoreCase = false)

        assertTrue(matcher.matchesContains("ABC"))
        assertTrue(matcher.matchesContains("xyz"))
        assertFalse(matcher.matchesContains("XYZ"))
        assertTrue(matcher.matches("ABC"))
        assertTrue(matcher.matches("xyz"))
        assertFalse(matcher.matches("XYZ"))
    }

    @Test
    fun `String matcher with contains should not match ignoring case`() {
        val matcher = StringMatcher(notContains = "Z", ignoreCase = true)

        assertTrue(matcher.matchesContains("ABC"))
        assertFalse(matcher.matchesContains("xyz"))
        assertFalse(matcher.matchesContains("XYZ"))
        assertTrue(matcher.matches("ABC"))
        assertFalse(matcher.matches("xyz"))
        assertFalse(matcher.matches("XYZ"))
    }

    @Test
    fun `String matcher with multiple requirements should match`() {
        val matcher = StringMatcher(
            equals = "ABC",
            matches = "^[A-Z]{3}$",
            notMatches = "^[A-Z]{4}$",
            contains = "A",
            notContains = "Z",
            ignoreCase = false,
        )

        assertTrue(matcher.matches("ABC"))
        assertFalse(matcher.matches("DEF"))
    }

    @Test
    fun `Deserializing string matcher should set correct properties`() {
        val value = """
            {
                "equals": "abc",
                "matches": "123",
                "not-matches": "456",
                "contains": "ABC",
                "not-contains": "999",
                "ignore-case": true
            }
        """.trimIndent()

        val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
            .enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER.mappedFeature())
        val result = mapper.readValue(value, StringMatcher::class.java)

        assertEquals("abc", result.equals)
        assertEquals("123", result.matches)
        assertEquals("456", result.notMatches)
        assertEquals("ABC", result.contains)
        assertEquals("999", result.notContains)
        assertTrue(result.ignoreCase)
    }

    @Test
    fun `Deserializing string matcher with regex should set correct properties`() {
        val value = """
            {
                "matches": "/account/(.*)/authorize$"
            }
        """.trimIndent()

        val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
            .enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER.mappedFeature())
        val result = mapper.readValue(value, StringMatcher::class.java)

        assertEquals("/account/(.*)/authorize$", result.matches)

        assertTrue(result.matches("/account/123/authorize"))
        assertFalse(result.matches("/account/123/authorize-xyz"))
    }

    @Test
    fun `Deserializing string matcher with regex with escaped fields should set correct properties`() {
        val value = """
            {
                "matches": "\\.authorize$"
            }
        """.trimIndent()

        val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
            .enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER.mappedFeature())
        val result = mapper.readValue(value, StringMatcher::class.java)

        assertEquals("\\.authorize$", result.matches)

        assertTrue(result.matches(".authorize"))
        assertFalse(result.matches(".authorize-xyz"))
    }

    @Test
    fun `Deserializing string matcher with regex with regex values should set correct properties`() {
        val value = """
            {
                "matches": "^\\d{3}$"
            }
        """.trimIndent()

        val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
            .enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER.mappedFeature())
        val result = mapper.readValue(value, StringMatcher::class.java)

        assertEquals("^\\d{3}$", result.matches)

        assertTrue(result.matches("123"))
        assertFalse(result.matches("1234"))
        assertFalse(result.matches("12-34"))
    }
}
