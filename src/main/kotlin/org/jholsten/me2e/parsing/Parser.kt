package org.jholsten.me2e.parsing

import org.jholsten.me2e.parsing.exception.ParseException
import kotlin.jvm.Throws

/**
 * Generic interface for services that parse and validate values to a model.
 */
internal interface Parser {

    /**
     * Parses the given value to the given model.
     * @param value Value to be parsed
     * @param clazz Model to which value should be parsed
     * @return Value parsed to the given model
     */
    @Throws(ParseException::class)
    fun <T> parse(value: String, clazz: Class<T>): T
}
