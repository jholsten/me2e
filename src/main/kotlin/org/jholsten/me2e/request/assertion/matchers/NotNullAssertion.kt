package org.jholsten.me2e.request.assertion.matchers

/**
 * Assertion for checking that a value of type [T] is not null.
 */
class NotNullAssertion<T> : Assertable<T?>(
    assertion = { actual -> actual != null },
    message = { property, actual -> "Expected $property\n\t$actual\nto not be null" }
)
