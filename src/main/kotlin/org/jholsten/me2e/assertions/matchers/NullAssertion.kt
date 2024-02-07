package org.jholsten.me2e.assertions.matchers

/**
 * Assertion for checking if a value of type [T] is null.
 * @param T Datatype of the value to check.
 */
class NullAssertion<T> : Assertable<T?>(
    assertion = { actual -> actual == null },
    message = "to be null",
) {
    override fun toString(): String = "null"
}
