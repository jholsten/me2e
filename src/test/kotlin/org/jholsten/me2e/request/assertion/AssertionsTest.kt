package org.jholsten.me2e.request.assertion

import org.jholsten.me2e.request.assertion.Assertions.Companion.contains
import org.jholsten.me2e.request.assertion.Assertions.Companion.containsKey
import org.jholsten.me2e.request.assertion.Assertions.Companion.isBetween
import org.jholsten.me2e.request.assertion.Assertions.Companion.isEqualTo
import org.jholsten.me2e.request.assertion.Assertions.Companion.isGreaterThan
import org.jholsten.me2e.request.assertion.Assertions.Companion.isLessThan
import org.jholsten.me2e.request.assertion.Assertions.Companion.isNotEqualTo
import org.jholsten.me2e.request.assertion.Assertions.Companion.isNotNull
import org.jholsten.me2e.request.assertion.Assertions.Companion.isNull
import org.jholsten.me2e.request.assertion.Assertions.Companion.matchesPattern
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
    fun `Inequality assertion should not throw for equal values`() {
        assertDoesNotThrow { isNotEqualTo("A").evaluate("Property", "B") }
        assertDoesNotThrow { isNotEqualTo(1).evaluate("Property", 2) }
        assertDoesNotThrow { isNotEqualTo(1.0).evaluate("Property", 2.0) }
        assertDoesNotThrow { isNotEqualTo(listOf("A")).evaluate("Property", listOf("B")) }
        assertDoesNotThrow { isNotEqualTo("A").evaluate("Property", null) }
    }

    @Test
    fun `Inequality assertion should throw for unequal values`() {
        assertFailsWith<AssertionFailure> { isNotEqualTo("A").evaluate("Property", "A") }
        assertFailsWith<AssertionFailure> { isNotEqualTo(1).evaluate("Property", 1) }
        assertFailsWith<AssertionFailure> { isNotEqualTo(1.0).evaluate("Property", 1.0) }
        assertFailsWith<AssertionFailure> { isNotEqualTo(listOf("A")).evaluate("Property", listOf("A")) }
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
    fun `Greater than assertion should not throw if actual is greater than expected`() {
        assertDoesNotThrow { isGreaterThan(10).evaluate("Property", 15) }
        assertDoesNotThrow { isGreaterThan(0.2).evaluate("Property", 1.0) }
    }

    @Test
    fun `Greater than assertion should throw if actual is not greater than expected`() {
        assertFailsWith<AssertionFailure> { isGreaterThan(10).evaluate("Property", 5) }
        assertFailsWith<AssertionFailure> { isGreaterThan(0.2).evaluate("Property", 0.1) }
        assertFailsWith<AssertionFailure> { isGreaterThan(10).evaluate("Property", null) }
    }

    @Test
    fun `Less than assertion should not throw if actual is less than expected`() {
        assertDoesNotThrow { isLessThan(10).evaluate("Property", 5) }
        assertDoesNotThrow { isLessThan(0.2).evaluate("Property", 0.1) }
    }

    @Test
    fun `Less than assertion should throw if actual is not less than expected`() {
        assertFailsWith<AssertionFailure> { isLessThan(10).evaluate("Property", 15) }
        assertFailsWith<AssertionFailure> { isLessThan(0.2).evaluate("Property", 1.0) }
        assertFailsWith<AssertionFailure> { isLessThan(10).evaluate("Property", null) }
    }

    @Test
    fun `Between assertion should not throw if actual is within range`() {
        assertDoesNotThrow { isBetween(10, 20).evaluate("Property", 15) }
        assertDoesNotThrow { isBetween(0.2, 2.0).evaluate("Property", 1.0) }
    }

    @Test
    fun `Between assertion should throw if actual is not within range`() {
        assertFailsWith<AssertionFailure> { isBetween(10, 20).evaluate("Property", 5) }
        assertFailsWith<AssertionFailure> { isBetween(0.2, 2.0).evaluate("Property", 3.0) }
        assertFailsWith<AssertionFailure> { isBetween(10, 20).evaluate("Property", null) }
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
