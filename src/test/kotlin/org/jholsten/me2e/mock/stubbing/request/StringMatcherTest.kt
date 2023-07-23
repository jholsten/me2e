package org.jholsten.me2e.mock.stubbing.request

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

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
    fun `String matcher with equals for url should match`() {
        val matcher = StringMatcher(equals = "/search")

        assertTrue(matcher.matches("/search", isUrl = true))
        assertTrue(matcher.matches("/search?name=dog", isUrl = true))
    }

    @Test
    fun `String matcher with pattern should match`() {
        val matcher = StringMatcher(matches = "^[A-Z0-9._-]{7}\$", ignoreCase = false)

        assertTrue(matcher.matchesPattern("A.1-2_3"))
        assertFalse(matcher.matchesPattern("a.1-2_3"))
        assertFalse(matcher.matchesPattern("nope"))
        assertTrue(matcher.matches("A.1-2_3"))
        assertFalse(matcher.matches("a.1-2_3"))
        assertFalse(matcher.matches("nope"))
    }

    @Test
    fun `String matcher with pattern should match ignoring case`() {
        val matcher = StringMatcher(matches = "^[A-Z0-9._-]{7}\$", ignoreCase = true)

        assertTrue(matcher.matchesPattern("A.1-2_3"))
        assertTrue(matcher.matchesPattern("a.1-2_3"))
        assertFalse(matcher.matchesPattern("nope"))
        assertTrue(matcher.matches("A.1-2_3"))
        assertTrue(matcher.matches("a.1-2_3"))
        assertFalse(matcher.matches("nope"))
    }

    @Test
    fun `String matcher with pattern for url should match`() {
        val matcher = StringMatcher(matches = "/[0-9]{3}")

        assertTrue(matcher.matches("/123", isUrl = true))
        assertTrue(matcher.matches("/123?name=dog", isUrl = true))
    }

    @Test
    fun `String matcher with pattern should not match`() {
        val matcher = StringMatcher(notMatches = "^[A-Z]{3}\$", ignoreCase = false)

        assertTrue(matcher.matchesPattern("abc"))
        assertFalse(matcher.matchesPattern("ABC"))
        assertTrue(matcher.matchesPattern("123"))
        assertTrue(matcher.matches("abc"))
        assertFalse(matcher.matches("ABC"))
        assertTrue(matcher.matches("123"))
    }

    @Test
    fun `String matcher with pattern should not match ignoring case`() {
        val matcher = StringMatcher(notMatches = "^[A-Z]{3}\$", ignoreCase = true)

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
            matches = "^[A-Z]{3}\$",
            notMatches = "^[A-Z]{4}\$",
            contains = "A",
            notContains = "Z",
            ignoreCase = false,
        )

        assertTrue(matcher.matches("ABC"))
        assertFalse(matcher.matches("DEF"))
    }
}
