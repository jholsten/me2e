package org.jholsten.me2e.request.assertion.matchers

/**
 * Assertion for checking if a value of type [T] is null.
 * @param T Datatype of the value to check.
 */
class NullAssertion<T> : Assertable<T?>(
    assertion = { actual -> actual == null },
    message = { property, actual -> "Expected $property\n\t$actual\nto be null" }
)
