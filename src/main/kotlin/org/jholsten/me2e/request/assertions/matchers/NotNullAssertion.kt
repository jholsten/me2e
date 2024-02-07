package org.jholsten.me2e.request.assertions.matchers

/**
 * Assertion for checking if a value of type [T] is not null.
 * @param T Datatype of the value to check.
 */
class NotNullAssertion<T> : Assertable<T?>(
    assertion = { actual -> actual != null },
    message = { property, actual -> "Expected $property\n\t$actual\nto not be null" }
)
