package org.jholsten.me2e.assertions.matchers

/**
 * Assertion for checking the equality of the contents of two byte arrays.
 */
class BinaryEqualityAssertion internal constructor(private val expected: ByteArray) : Assertable<ByteArray?>(
    assertion = { actual -> expected.contentEquals(actual) },
    message = "to be equal to\n\t${expected.asList()}",
    stringRepresentation = { actual -> actual?.asList()?.toString() },
) {
    override fun toString(): String = "equal to ${expected.asList()}"
}
