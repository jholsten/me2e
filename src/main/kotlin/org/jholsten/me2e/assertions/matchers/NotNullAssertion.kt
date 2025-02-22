package org.jholsten.me2e.assertions.matchers

/**
 * Assertion for checking if a value of type [T] is not null.
 * @param T Datatype of the value to check.
 */
class NotNullAssertion<T> internal constructor() : Assertable<T?>(
    assertion = { actual -> actual != null },
    message = "to not be null",
) {
    override fun toString(): String = "not null"
}
