package org.jholsten.me2e.assertions

import org.jholsten.me2e.assertions.Assertions.Companion.contains
import org.jholsten.me2e.assertions.Assertions.Companion.containsKey
import org.jholsten.me2e.assertions.Assertions.Companion.isEqualTo
import org.jholsten.me2e.assertions.Assertions.Companion.isNotNull
import org.jholsten.me2e.assertions.Assertions.Companion.isNull
import org.jholsten.me2e.assertions.Assertions.Companion.matchesPattern
import org.jholsten.util.assertDoesNotThrow
import kotlin.test.*

internal class AssertionsTest {

    @Test
    fun `Equality assertion should not throw for equal values`() {
        assertDoesNotThrow { isEqualTo("A").evaluate("Property", "A") }
        assertDoesNotThrow { isEqualTo(1).evaluate("Property", 1) }
        assertDoesNotThrow { isEqualTo(1.0).evaluate("Property", 1.0) }
        assertDoesNotThrow { isEqualTo(listOf("A")).evaluate("Property", listOf("A")) }
    }

    @Test
    fun `Equality assertion should throw for unequal values`() {
        assertFailsWith<AssertionFailure> { isEqualTo("A").evaluate("Property", "B") }
        assertFailsWith<AssertionFailure> { isEqualTo(1).evaluate("Property", 2) }
        assertFailsWith<AssertionFailure> { isEqualTo(1.0).evaluate("Property", 2.0) }
        assertFailsWith<AssertionFailure> { isEqualTo(listOf("A")).evaluate("Property", listOf("B")) }
        assertFailsWith<AssertionFailure> { isEqualTo("A").evaluate("Property", null) }
    }

    @Test
    fun `String contains assertion should not throw if actual contains expected`() {
        assertDoesNotThrow { contains("A").evaluate("Property", "ABCDEFGHIJKLMNOP") }
        assertDoesNotThrow { contains("ABC").evaluate("Property", "ABCDEFGHIJKLMNOP") }
    }

    @Test
    fun `String contains assertion should throw if actual does not contain expected`() {
        assertFailsWith<AssertionFailure> { contains("Z").evaluate("Property", "ABCDEFGHIJKLMNOP") }
        assertFailsWith<AssertionFailure> { contains("a").evaluate("Property", "ABCDEFGHIJKLMNOP") }
    }

    @Test
    fun `Multi map key contains assertion should not throw if actual contains key`() {
        assertDoesNotThrow { containsKey("A").evaluate("Property", mapOf("A" to listOf(1), "B" to listOf(2))) }
        assertDoesNotThrow { containsKey(1).evaluate("Property", mapOf(1 to listOf("A"), 2 to listOf("B"))) }
    }

    @Test
    fun `Multi map key contains assertion should throw if actual does not contain key`() {
        assertFailsWith<AssertionFailure> { containsKey("C").evaluate("Property", mapOf("A" to listOf(1), "B" to listOf(2))) }
        assertFailsWith<AssertionFailure> { containsKey(3).evaluate("Property", mapOf(1 to listOf("A"), 2 to listOf("B"))) }
    }

    @Test
    fun `Multi map value contains assertion should not throw if actual contains key with value`() {
        assertDoesNotThrow { containsKey("A").withValue(1).evaluate("Property", mapOf("A" to listOf(1), "B" to listOf(2))) }
        assertDoesNotThrow { containsKey("A").withValue(2).evaluate("Property", mapOf("A" to listOf(1, 2), "B" to listOf(2))) }
        assertDoesNotThrow { containsKey(1).withValue("A").evaluate("Property", mapOf(1 to listOf("A"), 2 to listOf("B"))) }
    }

    @Test
    fun `Multi map value contains assertion should throw if actual does not contain key with value`() {
        assertFailsWith<AssertionFailure> { containsKey("A").withValue(2).evaluate("Property", mapOf("A" to listOf(1), "B" to listOf(2))) }
        assertFailsWith<AssertionFailure> { containsKey(1).withValue("B").evaluate("Property", mapOf(1 to listOf("A"), 2 to listOf("B"))) }
        assertFailsWith<AssertionFailure> { containsKey("C").withValue(3).evaluate("Property", mapOf("A" to listOf(1), "B" to listOf(2))) }
        assertFailsWith<AssertionFailure> { containsKey(3).withValue("C").evaluate("Property", mapOf(1 to listOf("A"), 2 to listOf("B"))) }
    }

    @Test
    fun `Pattern match assertion should not throw if actual matches pattern`() {
        assertDoesNotThrow { matchesPattern("^[A-Z0-9._-]{7}\$").evaluate("Property", "A.1-2_3") }
        assertDoesNotThrow { matchesPattern(".*").evaluate("Property", "some-value") }
    }

    @Test
    fun `Pattern match assertion should throw if actual does not match pattern`() {
        assertFailsWith<AssertionFailure> { matchesPattern("^[A-Z0-9._-]{7}\$").evaluate("Property", "nope") }
        assertFailsWith<AssertionFailure> { matchesPattern("^[A-Z]{3}\$").evaluate("Property", "abc") }
    }

    @Test
    fun `Null assertion should not throw if actual is null`() {
        assertDoesNotThrow { isNull<String>().evaluate("Property", null) }
        assertDoesNotThrow { isNull<Int>().evaluate("Property", null) }
    }

    @Test
    fun `Null assertion should throw if actual is not null`() {
        assertFailsWith<AssertionFailure> { isNull<String>().evaluate("Property", "ABC") }
        assertFailsWith<AssertionFailure> { isNull<Int>().evaluate("Property", 0) }
    }

    @Test
    fun `Not null assertion should not throw if actual is not null`() {
        assertDoesNotThrow { isNotNull<String>().evaluate("Property", "ABC") }
        assertDoesNotThrow { isNotNull<Int>().evaluate("Property", 0) }
    }

    @Test
    fun `Not null assertion should throw if actual is null`() {
        assertFailsWith<AssertionFailure> { isNotNull<String>().evaluate("Property", null) }
        assertFailsWith<AssertionFailure> { isNotNull<Int>().evaluate("Property", null) }
    }
}
