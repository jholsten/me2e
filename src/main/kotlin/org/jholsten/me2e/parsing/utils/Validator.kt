package org.jholsten.me2e.parsing.utils

import org.jholsten.me2e.parsing.exception.ValidationException

/**
 * Interface for validating values of type [T].
 */
internal interface Validator<T> {

    /**
     * Validates the given [value].
     * @param value Value to validate.
     * @throws ValidationException if validation failed.
     */
    fun validate(value: T)
}
